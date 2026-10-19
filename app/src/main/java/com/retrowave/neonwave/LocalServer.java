package com.retrowave.neonwave;

import android.Manifest;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Restricted loopback-only HTTP streaming server. Never exposed to WiFi/mobile networks.
 * Uses a random per-process token. Supports HTTP Range for WebView seeking.
 */
public final class LocalServer implements Closeable {
    private static final String TAG="NEONWAVE";
    private final Context context;
    private final ServerSocket listener;
    private final ExecutorService executor=Executors.newFixedThreadPool(6);
    private final String token;
    private volatile boolean active;
    public LocalServer(Context ctx) throws Exception {
        context=ctx.getApplicationContext();
        byte[] random=new byte[24];new SecureRandom().nextBytes(random);
        StringBuilder value=new StringBuilder();for(byte b:random)value.append(String.format(Locale.US,"%02x",b&0xff));token=value.toString();
        listener=new ServerSocket(27814,16,InetAddress.getByName("127.0.0.1"));
    }
    public String url(){return "http://127.0.0.1:"+listener.getLocalPort()+"/?token="+token;}
    public void start(){active=true;Thread accept=new Thread(()->{while(active){try{Socket sock=listener.accept();sock.setSoTimeout(15000);executor.execute(()->serve(sock));}catch(Exception e){if(active)Log.w(TAG,"HTTP accept failed",e);}}},"NeonWaveListener");accept.setDaemon(true);accept.start();}
    private static String escapeQuery(String s){return s==null?"":s;}
    private static String queryValue(String uri,String key){int mark=uri.indexOf('?');if(mark<0)return "";String q=uri.substring(mark+1);for(String part:q.split("&")){String[] pair=part.split("=",2);if(pair[0].equals(key))return pair.length>1?pair[1]:"";}return "";}
    private void serve(Socket s){try(Socket socket=s;InputStream in=new BufferedInputStream(socket.getInputStream());OutputStream out=new BufferedOutputStream(socket.getOutputStream())) {
        ByteArrayOutputStream headers=new ByteArrayOutputStream();int prev=0,b=0;while(headers.size()<16384&&(b=in.read())!=-1){headers.write(b);if(prev=='\r'&&b=='\n'){byte[] a=headers.toByteArray();int len=a.length;if(len>=4&&a[len-4]=='\r'&&a[len-3]=='\n'&&a[len-2]=='\r'&&a[len-1]=='\n')break;}prev=b;}
        String raw=headers.toString("ISO-8859-1");String[] lines=raw.split("\r?\n");if(lines.length==0)return;String[] first=lines[0].split(" ");if(first.length<2||!first[0].equals("GET")){respond(out,405,"text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));return;}String url=first[1];String path=url.split("\\?",2)[0];if(!token.equals(queryValue(url,"token"))&&!path.equals("/style.css")&&!path.equals("/app.js")){respond(out,403,"text/plain","Forbidden".getBytes(StandardCharsets.UTF_8));return;}
        String range=null;for(int i=1;i<lines.length;i++){String l=lines[i];if(l.toLowerCase(Locale.US).startsWith("range:")){range=l.substring(6).trim();break;}}
        if(path.equals("/")||path.equals("/index.html")){asset(out,"index.html","text/html; charset=utf-8");return;}
        if(path.equals("/style.css")){asset(out,"style.css","text/css; charset=utf-8");return;}
        if(path.equals("/app.js")){asset(out,"app.js","application/javascript; charset=utf-8");return;}
        if(path.equals("/api/tracks")){tracks(out);return;}
        if(path.matches("/audio/n[0-9]+")){stream(out,path.substring(7),range);return;}
        respond(out,404,"text/plain","Not Found".getBytes(StandardCharsets.UTF_8));
    }catch(Exception e){Log.w(TAG,"HTTP connection",e);}}
    private void asset(OutputStream out,String asset,String mime) throws Exception{try(InputStream i=context.getAssets().open(asset)){ByteArrayOutputStream buffer=new ByteArrayOutputStream();byte[] block=new byte[8192];int n;while((n=i.read(block))!=-1)buffer.write(block,0,n);respond(out,200,mime,buffer.toByteArray());}}
    private void respond(OutputStream out,int code,String mime,byte[] bytes) throws Exception {String message=code==200?"OK":code==403?"Forbidden":code==404?"Not Found":code==405?"Method Not Allowed":"Error";writeHead(out,"HTTP/1.1 "+code+" "+message,"Content-Type: "+mime+"\r\nContent-Length: "+bytes.length+"\r\nCache-Control: no-store\r\n");out.write(bytes);out.flush();}
    private void writeHead(OutputStream out,String status,String heads) throws Exception {String h=status+"\r\n"+heads+"Connection: close\r\n\r\n";out.write(h.getBytes(StandardCharsets.US_ASCII));}
    private boolean hasAudioPermission(){return context.checkSelfPermission(Build.VERSION.SDK_INT>=33?Manifest.permission.READ_MEDIA_AUDIO:Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED;}
    private void tracks(OutputStream out) throws Exception {
        JSONObject response=new JSONObject();response.put("permission",hasAudioPermission());JSONArray list=new JSONArray();response.put("tracks",list);
        if(hasAudioPermission()){
            String[] columns=Build.VERSION.SDK_INT>=29?new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.SIZE,MediaStore.Audio.Media.MIME_TYPE,MediaStore.Audio.Media.RELATIVE_PATH}:new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.SIZE,MediaStore.Audio.Media.MIME_TYPE};
            try(Cursor cursor=context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,columns,MediaStore.Audio.Media.DURATION+">=?",new String[]{"30000"},MediaStore.Audio.Media.TITLE+" COLLATE NOCASE ASC")){
                if(cursor!=null){while(cursor.moveToNext()){
                    JSONObject t=new JSONObject();long id=cursor.getLong(0);String title=cursor.getString(1),file=cursor.getString(2),artist=cursor.getString(3),album=cursor.getString(4),mime=cursor.getString(7),folder=Build.VERSION.SDK_INT>=29?cursor.getString(8):"Music";
                    t.put("id","n"+id);t.put("title",(title!=null&&!title.trim().isEmpty())?title:file);t.put("artist",artist==null||artist.equals("<unknown>")?"Unknown artist":artist);
                    t.put("album",album==null||album.equals("<unknown>")?"Unknown album":album);t.put("folder",folder==null?"Music":folder);t.put("duration",cursor.getLong(5)/1000.0);t.put("size",cursor.getLong(6));t.put("mime",mime==null?"audio/*":mime);list.put(t);
                }}
            }catch(Exception e){Log.w(TAG,"MediaStore scan",e);response.put("error",e.getMessage());}
        }
        respond(out,200,"application/json; charset=utf-8",response.toString().getBytes(StandardCharsets.UTF_8));
    }
    private void stream(OutputStream out,String identifier,String range) throws Exception {
        if(!hasAudioPermission()){respond(out,403,"text/plain","Audio permission required".getBytes(StandardCharsets.UTF_8));return;}
        long id;
        try{id=Long.parseLong(identifier.substring(1));}catch(Exception e){respond(out,404,"text/plain",new byte[0]);return;}
        Uri uri=ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);
        try(ParcelFileDescriptor pfd=context.getContentResolver().openFileDescriptor(uri,"r")){
            if(pfd==null){respond(out,404,"text/plain",new byte[0]);return;}
            long total=pfd.getStatSize();String mime=context.getContentResolver().getType(uri);if(mime==null)mime="audio/mpeg";
            if(total<=0){try(Cursor c=context.getContentResolver().query(uri,new String[]{MediaStore.MediaColumns.SIZE},null,null,null)){if(c!=null&&c.moveToFirst())total=c.getLong(0);}}
            long start=0,end=total-1;
            if(range!=null&&range.startsWith("bytes=")&&total>0){
                String r=range.substring(6).split(",")[0];String[] parts=r.split("-",2);
                try{if(!parts[0].isEmpty())start=Long.parseLong(parts[0]);if(parts.length>1&&!parts[1].isEmpty())end=Math.min(Long.parseLong(parts[1]),total-1);}
                catch(NumberFormatException e){start=0;end=total-1;}
                if(start>=total||end<start){writeHead(out,"HTTP/1.1 416 Range Not Satisfiable","Content-Range: bytes */"+total+"\r\nContent-Length: 0\r\n");out.flush();return;}
            }
            long remaining=total>0?end-start+1:-1;boolean partial=range!=null&&total>0;
            String head="Content-Type: "+mime+"\r\nAccept-Ranges: bytes\r\nCache-Control: no-store\r\n"+(remaining>=0?"Content-Length: "+remaining+"\r\n":"")+(partial?"Content-Range: bytes "+start+"-"+end+"/"+total+"\r\n":"");
            writeHead(out,partial?"HTTP/1.1 206 Partial Content":"HTTP/1.1 200 OK",head);
            try(FileInputStream file=new FileInputStream(pfd.getFileDescriptor())){
                long toSkip=start;while(toSkip>0){long skipped=file.skip(toSkip);if(skipped<=0)break;toSkip-=skipped;}
                byte[] buf=new byte[32768];int n;
                while((n=file.read(buf,0,(int)(remaining>=0?Math.min((long)buf.length,remaining):buf.length)))>0){out.write(buf,0,n);if(remaining>=0){remaining-=n;if(remaining<=0)break;}}
            }out.flush();
        }catch(java.io.FileNotFoundException e){respond(out,404,"text/plain","Audio unavailable".getBytes(StandardCharsets.UTF_8));}
    }
    @Override public void close(){active=false;try{listener.close();}catch(Exception ignored){}executor.shutdownNow();}
}
