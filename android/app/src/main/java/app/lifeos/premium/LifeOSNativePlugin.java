package app.lifeos.premium;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResult;
import androidx.biometric.BiometricManager;
import androidx.core.content.FileProvider;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@CapacitorPlugin(name="LifeOSNative")
public class LifeOSNativePlugin extends Plugin {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private MainActivity host(){return (MainActivity)getActivity();}
    private boolean allowed(PluginCall c){if(!host().isAccessAllowed()){c.reject("LifeOS est verrouillé.");return false;}return true;}
    @PluginMethod public void status(PluginCall c){JSObject r=new JSObject();r.put("lockEnabled",BackupStore.prefs(getContext()).getBoolean("lock",false));r.put("canAuthenticate",BiometricManager.from(getContext()).canAuthenticate(MainActivity.AUTH)==BiometricManager.BIOMETRIC_SUCCESS);c.resolve(r);}
    @PluginMethod public void setLock(PluginCall c){if(!allowed(c))return;boolean enabled=Boolean.TRUE.equals(c.getBoolean("enabled"));getActivity().runOnUiThread(()->host().authenticate(()->{BackupStore.prefs(getContext()).edit().putBoolean("lock",enabled).apply();c.resolve();},c::reject));}
    @PluginMethod public void lockNow(PluginCall c){if(!allowed(c))return;if(!host().lockEnabled()){c.reject("Active d’abord le verrouillage.");return;}getActivity().runOnUiThread(()->{host().lockNow();c.resolve();});}
    @PluginMethod public void stageSnapshot(PluginCall c){if(!allowed(c))return;String data=c.getString("data");io.execute(()->{try{new org.json.JSONObject(data);BackupStore.put(getContext(),"snapshot",data);c.resolve();}catch(Exception e){c.reject("Copie Android impossible : "+e.getMessage());}});}
    @PluginMethod public void shareBackup(PluginCall c){
        if(!allowed(c))return;
        String data=c.getString("data"),name=c.getString("name","LifeOS-backup.json");
        if(data==null||data.length()>28_000_000){c.reject("Sauvegarde invalide ou trop volumineuse.");return;}
        io.execute(()->{try{
            new org.json.JSONObject(data);
            String safe=name.replaceAll("[^a-zA-Z0-9_.-]","_");
            File dir=new File(getContext().getCacheDir(),"shared-backups");
            if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("Dossier temporaire de partage inaccessible.");
            File file=new File(dir,safe);
            try(OutputStream out=new FileOutputStream(file)){out.write(data.getBytes(StandardCharsets.UTF_8));out.flush();}
            Uri uri=FileProvider.getUriForFile(getContext(),getContext().getPackageName()+".fileprovider",file);
            Intent send=new Intent(Intent.ACTION_SEND);send.setType("application/json");send.putExtra(Intent.EXTRA_STREAM,uri);
            send.setClipData(ClipData.newUri(getContext().getContentResolver(),"Sauvegarde LifeOS",uri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            getActivity().runOnUiThread(()->{try{startActivity(Intent.createChooser(send,"Partager la sauvegarde LifeOS"));c.resolve();}catch(Exception e){c.reject("Menu de partage indisponible : "+e.getMessage());}});
        }catch(Exception e){c.reject("Préparation du partage impossible : "+e.getMessage());}});
    }
    @PluginMethod public void saveBackup(PluginCall c){
        if(!allowed(c))return;
        String data=c.getString("data"),name=c.getString("name","LifeOS-backup.json");
        if(data==null||data.length()>28_000_000){c.reject("Sauvegarde invalide ou trop volumineuse.");return;}
        if(android.os.Build.VERSION.SDK_INT<29){exportFile(c);return;}
        io.execute(()->{android.net.Uri uri=null;try{
            org.json.JSONObject json=new org.json.JSONObject(data);
            String safe=name.replaceAll("[^a-zA-Z0-9_.-]","_");
            android.content.ContentValues values=new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,safe);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"application/json");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,android.os.Environment.DIRECTORY_DOWNLOADS+"/backup");
            values.put(android.provider.MediaStore.MediaColumns.IS_PENDING,1);
            uri=getContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(uri==null)throw new IOException("Android n’a pas créé le fichier dans Download/backup.");
            try(OutputStream out=getContext().getContentResolver().openOutputStream(uri,"w")){
                if(out==null)throw new IOException("Dossier de sauvegarde inaccessible.");
                out.write(data.getBytes(StandardCharsets.UTF_8));out.flush();
            }
            android.content.ContentValues ready=new android.content.ContentValues();ready.put(android.provider.MediaStore.MediaColumns.IS_PENDING,0);
            getContext().getContentResolver().update(uri,ready,null,null);
            JSObject result=new JSObject();result.put("path","Download/backup/"+safe);c.resolve(result);
        }catch(Exception e){if(uri!=null)getContext().getContentResolver().delete(uri,null,null);c.reject("Sauvegarde locale impossible : "+e.getMessage());}});
    }
    @PluginMethod public void exportFile(PluginCall c){if(!allowed(c))return;String data=c.getString("data"),name=c.getString("name","LifeOS-backup.json");if(data==null||data.length()>28_000_000){c.reject("Fichier invalide ou trop volumineux.");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,name.replaceAll("[^a-zA-Z0-9_.-]","_"));startActivityForResult(c,i,"exportResult");}
    @ActivityCallback private void exportResult(PluginCall c,ActivityResult r){if(c==null)return;if(r.getResultCode()!=Activity.RESULT_OK||r.getData()==null||r.getData().getData()==null){c.reject("Export annulé.");return;}io.execute(()->{try(OutputStream out=getContext().getContentResolver().openOutputStream(r.getData().getData(),"wt")){if(out==null)throw new IOException("Destination inaccessible");out.write(c.getString("data","").getBytes(StandardCharsets.UTF_8));c.resolve();}catch(Exception e){c.reject("Export impossible : "+e.getMessage());}});}
    @PluginMethod public void importFile(PluginCall c){if(!allowed(c))return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});startActivityForResult(c,i,"importResult");}
    @ActivityCallback private void importResult(PluginCall c,ActivityResult r){if(c==null)return;if(r.getResultCode()!=Activity.RESULT_OK||r.getData()==null||r.getData().getData()==null){c.reject("Import annulé.");return;}io.execute(()->{try(InputStream in=getContext().getContentResolver().openInputStream(r.getData().getData())){JSObject out=new JSObject();out.put("data",DriveFiles.readLimited(in));c.resolve(out);}catch(Exception e){c.reject("Lecture impossible : "+e.getMessage());}});}
}
