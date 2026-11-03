package com.emir.aistudio

import android.content.ContentValues
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

private val BG=Color(0xFF090B14); private val PANEL=Color(0xFF151A2B)
private val PURPLE=Color(0xFF8B5CF6); private val CYAN=Color(0xFF22D3EE)
private val WHITE=Color(0xFFF7F7FB); private val MUTED=Color(0xFFA5AEC5)

class MainActivity:ComponentActivity(){
 private fun prefs()=EncryptedSharedPreferences.create(this,"emir_ai_secure",
  MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
  EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
  EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM)
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  val p=prefs()
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=PURPLE,secondary=CYAN,background=BG,surface=PANEL,onBackground=WHITE,onSurface=WHITE)){
   Studio(p.getString("groq","")?:"",p.getString("gemini","")?:"",
    {g,m->p.edit().putString("groq",g).putString("gemini",m).apply()},
    {k,q->groq(k,q)},{k,q->video(k,q)},{b->save(b)})
  }}
 }
 private suspend fun groq(key:String,prompt:String)=withContext(Dispatchers.IO){
  val sys="""Sen profesyonel Türkçe 2D mobil simülasyon çizgi film senaristisin. Kullanıcı fikrini koru. Sevimli yuvarlak kafalı 2D karakterler, kalın siyah kontur, sade renkli arka plan, yandan sabit kamera, mobil oyun HUD. Bütün diyaloglar ve yazılar Türkçe. Karakter görünüşlerini tutarlı tarif et. Tek akıcı 6-10 saniyelik sahne için ayrıntılı video promptu yaz. Yalnızca promptu döndür."""
  val body=JSONObject().put("model","llama-3.3-70b-versatile").put("temperature",0.75)
   .put("messages",JSONArray().put(JSONObject().put("role","system").put("content",sys))
   .put(JSONObject().put("role","user").put("content",prompt)))
  val r=post("https://api.groq.com/openai/v1/chat/completions",body,"Bearer $key")
  r.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
   ?.takeIf{it.isNotBlank()}?:throw Exception("Groq yanıtında prompt bulunamadı.")
 }
 private suspend fun video(key:String,prompt:String)=withContext(Dispatchers.IO){
  val body=JSONObject().put("model","gemini-omni-1.1-flash").put("input",prompt)
   .put("response_format",JSONObject().put("type","video").put("aspect_ratio","9:16").put("resolution","720p"))
  val r=post("https://generativelanguage.googleapis.com/v1beta/interactions?key=$key",body,null)
  val steps=r.optJSONArray("steps")?:JSONArray()
  for(i in 0 until steps.length()){val s=steps.optJSONObject(i)?:continue
   if(s.optString("type")!="model_output")continue
   val arr=s.optJSONArray("content")?:continue
   for(j in 0 until arr.length()){val item=arr.optJSONObject(j)?:continue
    if(item.optString("type")=="video"){val d=item.optString("data");if(d.isNotBlank())return@withContext Base64.getDecoder().decode(d)}
   }
  }
  throw Exception("Gemini video döndürmedi. Model erişimini/kotayı kontrol et. Yanıt: ${r.toString().take(300)}")
 }
 private fun post(url:String,body:JSONObject,auth:String?):JSONObject{
  val c=URL(url).openConnection() as HttpURLConnection
  try{c.requestMethod="POST";c.connectTimeout=30000;c.readTimeout=300000;c.doOutput=true
   c.setRequestProperty("Content-Type","application/json");if(auth!=null)c.setRequestProperty("Authorization",auth)
   c.outputStream.use{it.write(body.toString().toByteArray(Charsets.UTF_8))}
   val code=c.responseCode;val stream=if(code in 200..299)c.inputStream else c.errorStream
   val txt=stream.bufferedReader().use{it.readText()}
   if(code !in 200..299){val msg=try{JSONObject(txt).optJSONObject("error")?.optString("message")}catch(_:Exception){null};throw Exception("API hatası ($code): ${msg?:txt.take(250)}")}
   JSONObject(txt)
  }finally{c.disconnect()}
 }
 private suspend fun save(bytes:ByteArray)=withContext(Dispatchers.IO){
  val name="EmirAI_${System.currentTimeMillis()}.mp4"
  val values=ContentValues().apply{put(MediaStore.MediaColumns.DISPLAY_NAME,name);put(MediaStore.MediaColumns.MIME_TYPE,"video/mp4");put(MediaStore.MediaColumns.RELATIVE_PATH,"Movies/EmirAIStudio")}
  val uri=contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values)?:throw Exception("Video dosyası oluşturulamadı.")
  contentResolver.openOutputStream(uri)?.use{it.write(bytes)}?:throw Exception("Video kaydedilemedi.")
  "Galeri > Movies/EmirAIStudio içine kaydedildi: $name"
 }
}

@Composable private fun Studio(g0:String,m0:String,saveKeys:(String,String)->Unit,
 makeScript:suspend(String,String)->String,makeVideo:suspend(String,String)->ByteArray,saveVideo:suspend(ByteArray)->String){
 var groq by remember{mutableStateOf(g0)};var gemini by remember{mutableStateOf(m0)}
 var idea by remember{mutableStateOf("Emir ailesi tarafından evden kovulur. Elinde küçük bir çantayla sokağa çıkar ve yeni bir hayat kurmaya karar verir.")}
 var prompt by remember{mutableStateOf("")};var status by remember{mutableStateOf("Hazır • 2D çizgi film stüdyosu")}
 var busy by remember{mutableStateOf(false)};var keys by remember{mutableStateOf(false)}
 val scope= rememberCoroutineScope()
 Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF20163D),BG,Color(0xFF080D19))))){
  Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(15.dp)){
   AnimatedVisibility(true,enter=fadeIn()+slideInVertically()){Column{
    Text("EMIR AI STUDIO",fontSize=28.sp,fontWeight=FontWeight.Black,color=WHITE,letterSpacing=1.1.sp)
    Text("FİKRİNİ 2D ÇİZGİ FİLME DÖNÜŞTÜR",fontSize=11.sp,fontWeight=FontWeight.Bold,color=CYAN,letterSpacing=1.4.sp)
   }}
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Chip("2D CARTOON",PURPLE);Chip("DİKEY 9:16",CYAN);Chip("MP4",Color(0xFF34D399))}
   PanelCard{
    Text("🎬  Video fikrin",fontSize=18.sp,fontWeight=FontWeight.Bold)
    Text("Hikâyeni yaz. Groq bunu video promptuna dönüştürür.",fontSize=13.sp,color=MUTED)
    OutlinedTextField(idea,{idea=it},modifier=Modifier.fillMaxWidth().heightIn(min=120.dp),placeholder={Text("Örn: Emir ilk iş gününe başlar...")},shape=RoundedCornerShape(15.dp),colors=fields())
    Button(onClick={
     if(groq.isBlank()){status="Önce Groq API anahtarını ekle.";return@Button}
     busy=true;status="Groq senaryonu hazırlıyor..."
     scope.launch{try{prompt=makeScript(groq,idea);status="Prompt hazır! Video üretebilirsin."}catch(e:Exception){status=e.message?: "Senaryo oluşturulamadı."}finally{busy=false}}
    },enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=PURPLE)){
     Text("✦  SENARYO / PROMPT OLUŞTUR",fontWeight=FontWeight.Bold)
    }
   }
   PanelCard{
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Video promptu",fontSize=17.sp,fontWeight=FontWeight.Bold);Text("2D",color=CYAN,fontWeight=FontWeight.Bold)}
    OutlinedTextField(prompt,{prompt=it},modifier=Modifier.fillMaxWidth().heightIn(min=125.dp),placeholder={Text("Groq promptu burada görünecek. Düzenleyebilirsin.")},shape=RoundedCornerShape(15.dp),colors=fields())
    Button(onClick={
     if(gemini.isBlank()){status="Önce Gemini API anahtarını ekle.";return@Button}
     if(prompt.isBlank()){status="Önce prompt oluştur.";return@Button}
     busy=true;status="Gemini video oluşturuyor. Birkaç dakika sürebilir..."
     scope.launch{try{status=saveVideo(makeVideo(gemini,prompt))}catch(e:Exception){status=e.message?: "Video üretilemedi."}finally{busy=false}}
    },enabled=!busy,modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(15.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0E9F9A))){
     if(busy)CircularProgressIndicator(Modifier.size(18.dp),color=WHITE,strokeWidth=2.dp) else Text("▶  VİDEOYU OLUŞTUR VE KAYDET",fontWeight=FontWeight.Bold)
    }
   }
   PanelCard{
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
     Text("API AYARLARI",fontWeight=FontWeight.Bold,fontSize=17.sp)
     TextButton(onClick={keys=!keys}){Text(if(keys)"Gizle" else "Düzenle")}
    }
    Text("Anahtarlar telefonda şifreli saklanır.",fontSize=12.sp,color=MUTED)
    if(keys){
     OutlinedTextField(groq,{groq=it},modifier=Modifier.fillMaxWidth(),label={Text("Groq API Key")},visualTransformation=PasswordVisualTransformation(),colors=fields())
     OutlinedTextField(gemini,{gemini=it},modifier=Modifier.fillMaxWidth(),label={Text("Gemini API Key")},visualTransformation=PasswordVisualTransformation(),colors=fields())
     Button(onClick={saveKeys(groq.trim(),gemini.trim());keys=false;status="API anahtarları kaydedildi."},modifier=Modifier.fillMaxWidth()){Text("ANAHTARLARI KAYDET")}
    }
   }
   PanelCard{
    Text("İŞLEM DURUMU",color=CYAN,fontWeight=FontWeight.Bold,fontSize=12.sp)
    Text(status,color=WHITE,fontSize=14.sp)
    if(busy)LinearProgressIndicator(Modifier.fillMaxWidth(),color=CYAN)
   }
   Text("Bilgi: Gemini Omni kısa klipler (3–10 sn) üretir. 60 saniyelik bölüm için sahneleri ayrı üretip birleştirmek gerekir. API kullanımı kota/ücret doğurabilir.",color=MUTED,fontSize=11.sp)
   Spacer(Modifier.height(16.dp))
  }
 }
}
@Composable private fun PanelCard(content:@Composable ColumnScope.()->Unit){
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=PANEL.copy(alpha=.97f)),elevation=CardDefaults.cardElevation(5.dp)){
  Column(Modifier.padding(17.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
 }
}
@Composable private fun Chip(s:String,c:Color){Surface(color=c.copy(alpha=.15f),shape=RoundedCornerShape(50)){Text(s,Modifier.padding(horizontal=11.dp,vertical=7.dp),color=c,fontSize=10.sp,fontWeight=FontWeight.ExtraBold)}}
@Composable private fun fields()=OutlinedTextFieldDefaults.colors(focusedBorderColor=CYAN,unfocusedBorderColor=Color(0xFF3B435B),focusedTextColor=WHITE,unfocusedTextColor=WHITE,focusedLabelColor=CYAN,unfocusedLabelColor=MUTED,cursorColor=CYAN)
