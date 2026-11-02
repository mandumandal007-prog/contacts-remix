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
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) showApp()
    }
    private fun showApp() = setContent {
        var refresh by remember { mutableIntStateOf(0) }
        var dark by remember { mutableStateOf(false) }
        val contacts = remember(refresh) { loadContacts() }
        val scheme = if (dark) darkColorScheme(primary = Color(0xFFFF8A80), background = Color(0xFF141414), surface = Color(0xFF202020))
            else lightColorScheme(primary = Color(0xFFB3261E), background = Color(0xFFFAF8F6), surface = Color.White)
        MaterialTheme(colorScheme = scheme) {
            ContactsHome(
                contacts = contacts, dark = dark, onToggleDark = { dark = it }, onRefresh = { refresh++ },
                onCall = { number -> startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))) },
                onMessage = { number -> startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))) },
                onAdd = { startActivity(Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)) },
                onEdit = { id -> startActivity(Intent(Intent.ACTION_EDIT, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id))) }
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
private fun ContactsHome(contacts: List<Person>, dark: Boolean, onToggleDark: (Boolean) -> Unit, onRefresh: () -> Unit, onCall: (String) -> Unit, onMessage: (String) -> Unit, onAdd: () -> Unit, onEdit: (Long) -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var dial by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Person?>(null) }
    var settings by remember { mutableStateOf(false) }
    val filtered = contacts.filter { it.name.contains(query, true) || it.phone.contains(query) }
    val pageTitle = if (settings) "Settings" else selected?.name ?: listOf("Contacts", "Favorites", "Recents", "Keypad")[tab]
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        Row(Modifier.fillMaxWidth().padding(start = 22.dp, end = 18.dp, top = 20.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected != null || settings) Text("‹", fontSize = 32.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { selected = null; settings = false }.padding(end = 12.dp))
            Text(pageTitle, fontSize = 29.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            if (selected == null && !settings) Text("⚙", fontSize = 22.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.clickable { settings = true }.padding(8.dp))
        }
    }, bottomBar = {
        if (selected == null && !settings) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            listOf("Contacts", "Favorites", "Recents", "Keypad").forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Text(listOf("●", "★", "◷", "▦")[i], fontSize = 20.sp) }, label = { Text(label) }) }
        }
    }, floatingActionButton = { if (selected == null && !settings && (tab == 0 || tab == 1)) FloatingActionButton(onClick = onAdd, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White) { Text("＋", fontSize = 24.sp) } }) { pad ->
        when {
            settings -> SettingsScreen(dark, onToggleDark, onRefresh, Modifier.padding(pad))
            selected != null -> CallingCard(selected!!, onCall, onMessage, onEdit, Modifier.padding(pad))
            tab == 3 -> Keypad(dial, { dial = it }, onCall, Modifier.padding(pad))
            tab == 2 -> Column(Modifier.fillMaxSize().padding(pad).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Recent calls", fontSize = 22.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)); Text("Call history is available in your phone app. Tap a contact or use the keypad to start a call.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> {
                Column(Modifier.fillMaxSize().padding(pad)) {
                    OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp), placeholder = { Text("Search contacts") }, singleLine = true, shape = RoundedCornerShape(16.dp), leadingIcon = { Text("⌕", fontSize = 23.sp) })
                    val rows = if (tab == 1) filtered.filter { it.starred } else filtered
                    if (rows.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (tab == 1) "No favorites yet" else "No contacts found", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    else LazyColumn(Modifier.fillMaxSize()) {
                        items(rows, key = { it.id }) { p ->
                            Row(Modifier.fillMaxWidth().clickable { selected = p }.padding(horizontal = 18.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                                Avatar(p.name, 48)
                                Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(p.name, fontSize = 16.sp, fontWeight = FontWeight.Medium); Text(p.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                if (p.starred) Text("★", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                                Text("☎", color = MaterialTheme.colorScheme.primary, fontSize = 19.sp, modifier = Modifier.clickable { onCall(p.phone) }.padding(10.dp))
                            }
                            HorizontalDivider(Modifier.padding(start = 80.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Avatar(name: String, size: Int) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(Color(0xFFFCE4E1)), contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase().ifBlank { "?" }, color = Color(0xFF9F211B), fontWeight = FontWeight.SemiBold, fontSize = (size / 2.5).sp)
    }
}

@Composable
private fun CallingCard(person: Person, onCall: (String) -> Unit, onMessage: (String) -> Unit, onEdit: (Long) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(26.dp)); Avatar(person.name, 112); Spacer(Modifier.height(18.dp))
        Text(person.name, fontSize = 27.sp, fontWeight = FontWeight.SemiBold); Text(if (person.starred) "★ Favorite" else "Contact", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionButton("☎", "Call", Modifier.weight(1f)) { onCall(person.phone) }; ActionButton("✉", "Message", Modifier.weight(1f)) { onMessage(person.phone) }; ActionButton("✎", "Edit", Modifier.weight(1f)) { onEdit(person.id) }
        }
        Spacer(Modifier.height(28.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(18.dp)) { Text("Contact details", fontWeight = FontWeight.SemiBold, fontSize = 18.sp); Spacer(Modifier.height(14.dp)); Text("Mobile", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp); Text(person.phone, fontSize = 17.sp, modifier = Modifier.clickable { onCall(person.phone) }.padding(top = 4.dp)) }
        }
    }
}

@Composable
private fun ActionButton(icon: String, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface).clickable(onClick = onClick).padding(vertical = 15.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 23.sp, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(5.dp)); Text(label, fontSize = 13.sp)
    }
}

@Composable
private fun SettingsScreen(dark: Boolean, onToggleDark: (Boolean) -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Display", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Dark theme", fontWeight = FontWeight.Medium); Text("Use a darker appearance", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Switch(checked = dark, onCheckedChange = onToggleDark)
            }
        }
        Text("App", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Contacts Remix", fontWeight = FontWeight.Medium); Spacer(Modifier.height(4.dp)); Text("Version 1.0 • Android 13 and newer", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(vertical = 12.dp)); Text("Your contacts stay on this device. Calls and messages open your phone's system apps.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Reference", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 24.dp, bottom = 6.dp))
        Text("Original implementation inspired by public descriptions of OnePlus Contacts. Not affiliated with OnePlus and does not include its proprietary code or assets.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp)); TextButton(onClick = onRefresh) { Text("Refresh contacts") }
    }
}

@Composable
private fun Keypad(dial: String, onDialChange: (String) -> Unit, onCall: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(dial, onDialChange, Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Enter number") }, singleLine = true)
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
        Column(Modifier.fillMaxWidth().weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.Center) {
            keys.chunked(3).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { row.forEach { key -> Surface(Modifier.padding(7.dp).size(68.dp).clickable { onDialChange(dial + key) }, shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) { Box(contentAlignment = Alignment.Center) { Text(key, fontSize = 24.sp) } } } } }
            Button(onClick = { if (dial.isNotBlank()) onCall(dial) }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF249B5B))) { Text("Call") }
        }
    }
}
