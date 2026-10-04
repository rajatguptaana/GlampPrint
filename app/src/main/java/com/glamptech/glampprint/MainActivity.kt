package com.glamptech.glampprint

import android.app.PendingIntent
import android.content.*
import android.hardware.usb.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF000000)
private val Card = Color(0xFF101312)
private val Card2 = Color(0xFF151917)
private val Emerald = Color(0xFF34D399)
private val Text = Color(0xFFF4F4F5)
private val Muted = Color(0xFF85898A)
private val Border = Color.White.copy(alpha = .09f)

class MainActivity : ComponentActivity() {
    private lateinit var usb: UsbManager
    private var devices by mutableStateOf(listOf<UsbDevice>())
    private val action = "com.glamptech.glampprint.USB_PERMISSION"

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == action) refreshUsb()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usb = getSystemService(USB_SERVICE) as UsbManager
        registerReceiver(receiver, IntentFilter(action), RECEIVER_NOT_EXPORTED)
        refreshUsb()
        setContent { GlampPrintApp(devices, ::requestPermission, ::refreshUsb) }
    }
    private fun refreshUsb() { devices = usb.deviceList.values.toList() }
    private fun requestPermission(device: UsbDevice) {
        val pi = PendingIntent.getBroadcast(this, 0, Intent(action), PendingIntent.FLAG_IMMUTABLE)
        usb.requestPermission(device, pi)
    }
    override fun onDestroy() { unregisterReceiver(receiver); super.onDestroy() }
}

@Composable fun GlampPrintApp(devices: List<UsbDevice>, request: (UsbDevice)->Unit, refresh: ()->Unit) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(containerColor = Black, bottomBar = { BottomBar(tab) { tab = it } }) { pad ->
        AnimatedContent(tab, modifier = Modifier.padding(pad), label = "screen") { page ->
            when(page) {
                0 -> Home(devices, request, refresh)
                1 -> Printers(devices, request, refresh)
                2 -> History()
                else -> More()
            }
        }
    }
}

@Composable fun BottomBar(tab:Int, onTab:(Int)->Unit) {
    NavigationBar(containerColor = Color(0xFF090A0A), tonalElevation = 0.dp) {
        listOf(Icons.Default.Home to "Home", Icons.Default.Print to "Printers", Icons.Default.History to "History", Icons.Default.MoreHoriz to "More").forEachIndexed { i,(icon,label) ->
            NavigationBarItem(selected=tab==i,onClick={onTab(i)},icon={Icon(icon,label)},label={Text(label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Black,selectedTextColor=Emerald,indicatorColor=Emerald.copy(.9f),unselectedIconColor=Muted,unselectedTextColor=Muted))
        }
    }
}

@Composable fun Header(title:String, subtitle:String?=null) {
    Column(Modifier.fillMaxWidth().padding(horizontal=22.dp, vertical=18.dp)) {
        Text(title,fontSize=28.sp,fontWeight=FontWeight.SemiBold,color=Text,letterSpacing=(-1).sp)
        subtitle?.let { Text(it,fontSize=12.sp,color=Muted,modifier=Modifier.padding(top=5.dp)) }
    }
}

@Composable fun GlassCard(modifier:Modifier=Modifier, content:@Composable ColumnScope.()->Unit) {
    Column(modifier.background(Brush.verticalGradient(listOf(Card2,Card)),RoundedCornerShape(24.dp)).border(1.dp,Border,RoundedCornerShape(24.dp)).padding(18.dp),content=content)
}

@Composable fun Home(devices:List<UsbDevice>, request:(UsbDevice)->Unit, refresh:()->Unit) {
    LazyColumn(contentPadding=PaddingValues(bottom=24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item { Header("GlampPrint","Print. Simply.") }
        item { GlassCard(Modifier.padding(horizontal=18.dp).fillMaxWidth()) {
            Row(verticalAlignment=Alignment.CenterVertically) { Icon(Icons.Default.Print,null,tint=Emerald,modifier=Modifier.size(32.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)){Text(if(devices.isNotEmpty()) "USB Printer Found" else "No Printer Connected",color=Text,fontWeight=FontWeight.SemiBold);Text(if(devices.isNotEmpty()) "OTG • Ready to connect" else "Connect a printer via OTG",color=Muted,fontSize=12.sp)}; Text("●",color=if(devices.isNotEmpty()) Emerald else Muted) }
        } }
        item { Row(Modifier.padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            ActionCard("Quick Print",Icons.Default.Bolt,Modifier.weight(1f))
            ActionCard("Receipt",Icons.Default.ReceiptLong,Modifier.weight(1f))
        } }
        item { Row(Modifier.padding(horizontal=18.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            SmallCard("PDF Print",Icons.Default.PictureAsPdf,Modifier.weight(1f)); SmallCard("Image Print",Icons.Default.Image,Modifier.weight(1f)); SmallCard("QR / Barcode",Icons.Default.QrCode,Modifier.weight(1f))
        } }
        item { Text("RECENT JOBS",fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=2.sp,color=Muted,modifier=Modifier.padding(horizontal=22.dp,vertical=8.dp)) }
        item { Job("Invoice.pdf","2 min ago","Completed") }
        item { Job("Receipt.png","Today","Completed") }
    }
}

@Composable fun ActionCard(title:String,icon: androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier) { GlassCard(modifier.height(105.dp).clickable {}) { Icon(icon,null,tint=Emerald,modifier=Modifier.size(24.dp)); Spacer(Modifier.height(14.dp)); Text(title,color=Text,fontWeight=FontWeight.Medium); Text("Tap to start",color=Muted,fontSize=10.sp) } }
@Composable fun SmallCard(title:String,icon:androidx.compose.ui.graphics.vector.ImageVector,modifier:Modifier){ GlassCard(modifier.height(100.dp).clickable {}) { Icon(icon,null,tint=Muted,modifier=Modifier.size(20.dp)); Spacer(Modifier.height(12.dp)); Text(title,color=Text,fontSize=11.sp,fontWeight=FontWeight.Medium) } }
@Composable fun Job(name:String,time:String,status:String){ GlassCard(Modifier.padding(horizontal=18.dp).fillMaxWidth().height(72.dp)){Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Description,null,tint=Emerald,modifier=Modifier.size(22.dp));Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(name,color=Text,fontSize=13.sp);Text(time,color=Muted,fontSize=10.sp)};Text(status,color=Emerald,fontSize=10.sp)}}}

@Composable fun Printers(devices:List<UsbDevice>,request:(UsbDevice)->Unit,refresh:()->Unit){
    Column { Header("Printers","USB / OTG printer management"); Button(onClick=refresh,modifier=Modifier.padding(horizontal=18.dp).fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Emerald,contentColor=Black),shape=RoundedCornerShape(50)){Icon(Icons.Default.Refresh,null);Spacer(Modifier.width(8.dp));Text("Scan USB Printers",fontWeight=FontWeight.Bold)}; Spacer(Modifier.height(14.dp)); if(devices.isEmpty()) GlassCard(Modifier.padding(18.dp).fillMaxWidth()){Text("No USB printer detected",color=Text,fontWeight=FontWeight.Medium);Text("Connect the printer through an OTG adapter and scan again.",color=Muted,fontSize=12.sp,modifier=Modifier.padding(top=6.dp))} else devices.forEach{d->GlassCard(Modifier.padding(horizontal=18.dp,vertical=5.dp).fillMaxWidth()){Text(d.productName ?: "USB Printer",color=Text,fontWeight=FontWeight.SemiBold);Text("VID ${d.vendorId} • PID ${d.productId}",color=Muted,fontSize=11.sp);Spacer(Modifier.height(12.dp));Button(onClick={request(d)},colors=ButtonDefaults.buttonColors(containerColor=Emerald,contentColor=Black),modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(50)){Text("Connect")}}} }
}

@Composable fun History(){Column{Header("History","Your recent print jobs");Job("Invoice.pdf","Today • 1 copy","Completed");Job("Ticket.pdf","Yesterday • 2 copies","Completed");Job("Receipt.png","Yesterday • 1 copy","Completed")}}
@Composable fun More(){Column{Header("More","Tools & settings");listOf("Receipt Studio","QR & Barcode","Print Queue","Business Mode","Printer Diagnostics","Settings").forEach{label->GlassCard(Modifier.padding(horizontal=18.dp,vertical=5.dp).fillMaxWidth().clickable{}){Row(verticalAlignment=Alignment.CenterVertically){Text(label,color=Text,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f));Icon(Icons.Default.ChevronRight,null,tint=Muted)}}}}}
