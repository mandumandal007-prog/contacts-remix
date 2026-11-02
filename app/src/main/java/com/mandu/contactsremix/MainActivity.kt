package com.mandu.contactsremix

import android.Manifest
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

data class Person(val id: Long, val name: String, val phone: String, val starred: Boolean)

class MainActivity : ComponentActivity() {
    private val askContacts = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) showApp()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) showApp()
        else askContacts.launch(Manifest.permission.READ_CONTACTS)
    }
    private fun showApp() = setContent {
        MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFFDB3D36), background = Color(0xFFF7F7F7), surface = Color.White)) {
            ContactsHome(
                contacts = remember { loadContacts() },
                onCall = { number -> startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))) },
                onOpen = { id -> startActivity(Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id))) },
                onAdd = { startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)) },
                onEdit = { startActivity(Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)) }
            )
        }
    }
    private fun loadContacts(): List<Person> {
        val out = mutableListOf<Person>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.STARRED), null, null, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE LOCALIZED ASC")?.use { c ->
            val idIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val starIx = c.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.STARRED)
            val seen = hashSetOf<Long>()
            while (c.moveToNext()) {
                val id = c.getLong(idIx)
                if (seen.add(id)) out += Person(id, c.getString(nameIx) ?: "", c.getString(numIx) ?: "", c.getInt(starIx) != 0)
            }
        }
        return out
    }
}

@Composable
private fun ContactsHome(contacts: List<Person>, onCall: (String)->Unit, onOpen: (Long)->Unit, onAdd: ()->Unit, onEdit: ()->Unit) {
    var tab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }
    var dial by remember { mutableStateOf("") }
    val filtered = contacts.filter { it.name.contains(query, true) || it.phone.contains(query) }
    Scaffold(containerColor = Color(0xFFF7F7F7), bottomBar = {
        NavigationBar(containerColor = Color.White) {
            listOf("Contacts", "Favorites", "Recents", "Keypad").forEachIndexed { i, label ->
                NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(listOf("●", "★", "◷", "▦")[i], fontSize = 20.sp) }, label = { Text(label) })
            }
        }
    }, floatingActionButton = { if(tab == 0 || tab == 1) FloatingActionButton(onClick = onAdd, containerColor = Color(0xFFDB3D36), contentColor = Color.White) { Text("＋", fontSize = 24.sp) } }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(listOf("Contacts", "Favorites", "Recents", "Keypad")[tab], fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("⋮", fontSize = 26.sp, color = Color.DarkGray, modifier = Modifier.clickable { onEdit() }.padding(8.dp))
            }
            if (tab == 3) {
                OutlinedTextField(value = dial, onValueChange = { dial = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), label = { Text("Enter number") }, singleLine = true)
                val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
                Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 34.dp), verticalArrangement = Arrangement.Center) {
                    keys.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { k -> Surface(Modifier.padding(7.dp).size(70.dp).clickable { dial += k }, shape = CircleShape, color = Color.White, shadowElevation = 2.dp) { Box(contentAlignment = Alignment.Center) { Text(k, fontSize = 24.sp) } } } } }
                    Button(onClick = { if (dial.isNotBlank()) onCall(dial) }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF249B5B))) { Text("Call") }
                }
            } else {
                OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), placeholder = { Text("Search contacts") }, singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Text("⌕", fontSize = 23.sp) })
                val rows = when(tab) { 1 -> filtered.filter { it.starred }; else -> filtered }
                LazyColumn(Modifier.fillMaxSize()) {
                    if(rows.isEmpty()) item { Text(if(tab == 2) "Recent calls are in your phone app" else "No contacts found", modifier = Modifier.padding(24.dp), color = Color.Gray) }
                    items(rows, key = { it.id }) { p ->
                        Row(Modifier.fillMaxWidth().clickable { onOpen(p.id) }.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFE3E0)), contentAlignment = Alignment.Center) { Text(p.name.take(1).uppercase().ifBlank { "?" }, color = Color(0xFFB92C27), fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
                            Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text(p.phone, fontSize = 13.sp, color = Color.Gray) }
                            if(p.starred) Text("★", color = Color(0xFFDB3D36), modifier = Modifier.padding(end = 12.dp))
                            Text("☎", color = Color(0xFFDB3D36), fontSize = 19.sp, modifier = Modifier.clickable { onCall(p.phone) }.padding(10.dp))
                        }
                        HorizontalDivider(Modifier.padding(start = 80.dp), color = Color(0xFFECECEC))
                    }
                }
            }
        }
    }
}
