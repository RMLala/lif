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
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

@CapacitorPlugin(name="LifeOSNative")
public class LifeOSNativePlugin extends Plugin {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private MainActivity host(){return (MainActivity)getActivity();}
    private boolean allowed(PluginCall c){if(!host().isAccessAllowed()){c.reject("LifeOS est verrouillé.");return false;}return true;}
        private static final String CLOUD_TOKEN_PREF="cloud_token_v1",CLOUD_TOKEN_ALIAS="lifeos_cloud_token_v1";
    private SecretKey cloudKey() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);
        if(ks.containsAlias(CLOUD_TOKEN_ALIAS))return (SecretKey)ks.getKey(CLOUD_TOKEN_ALIAS,null);
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(CLOUD_TOKEN_ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return gen.generateKey();
    }
    @PluginMethod public void storeCloudToken(PluginCall c){
        if(!allowed(c))return;String token=c.getString("token");
        if(token==null||token.length()<20||token.length()>4096){c.reject("Jeton Cloud invalide.");return;}
        io.execute(()->{try{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,cloudKey());byte[] encrypted=cipher.doFinal(token.getBytes(StandardCharsets.UTF_8));byte[] iv=cipher.getIV();byte[] packed=new byte[iv.length+encrypted.length];System.arraycopy(iv,0,packed,0,iv.length);System.arraycopy(encrypted,0,packed,iv.length,encrypted.length);getContext().getSharedPreferences("lifeos.secure",android.content.Context.MODE_PRIVATE).edit().putString(CLOUD_TOKEN_PREF,Base64.encodeToString(packed,Base64.NO_WRAP)).apply();c.resolve();}catch(Exception e){c.reject("Stockage sécurisé du jeton impossible.");}});
    }
    @PluginMethod public void getCloudToken(PluginCall c){
        if(!allowed(c))return;
        io.execute(()->{try{String stored=getContext().getSharedPreferences("lifeos.secure",android.content.Context.MODE_PRIVATE).getString(CLOUD_TOKEN_PREF,null);JSObject result=new JSObject();if(stored==null){result.put("token","");c.resolve(result);return;}byte[] packed=Base64.decode(stored,Base64.NO_WRAP);if(packed.length<29)throw new IllegalArgumentException();byte[] iv=java.util.Arrays.copyOfRange(packed,0,12),encrypted=java.util.Arrays.copyOfRange(packed,12,packed.length);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,cloudKey(),new GCMParameterSpec(128,iv));result.put("token",new String(cipher.doFinal(encrypted),StandardCharsets.UTF_8));c.resolve(result);}catch(Exception e){getContext().getSharedPreferences("lifeos.secure",android.content.Context.MODE_PRIVATE).edit().remove(CLOUD_TOKEN_PREF).apply();c.reject("Jeton Cloud illisible; il sera renouvelé après vérification de la licence.");}});
    }
    @PluginMethod public void clearCloudToken(PluginCall c){
        if(!allowed(c))return;getContext().getSharedPreferences("lifeos.secure",android.content.Context.MODE_PRIVATE).edit().remove(CLOUD_TOKEN_PREF).apply();c.resolve();
    }
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
            getActivity().runOnUiThread(()->{try{getActivity().startActivity(Intent.createChooser(send,"Partager la sauvegarde LifeOS"));c.resolve();}catch(Exception e){c.reject("Menu de partage indisponible : "+e.getMessage());}});
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
    @ActivityCallback private void importResult(PluginCall c,ActivityResult r){if(c==null)return;if(r.getResultCode()!=Activity.RESULT_OK||r.getData()==null||r.getData().getData()==null){c.reject("Import annulé.");return;}io.execute(()->{try(InputStream in=getContext().getContentResolver().openInputStream(r.getData().getData())){JSObject out=new JSObject();out.put("data",BackupFileReader.readLimited(in));c.resolve(out);}catch(Exception e){c.reject("Lecture impossible : "+e.getMessage());}});}
}
