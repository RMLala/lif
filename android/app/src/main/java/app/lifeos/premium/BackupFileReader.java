package app.lifeos.premium;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class BackupFileReader {
    private BackupFileReader(){}
    public static String readLimited(InputStream in)throws IOException{
        if(in==null)throw new IOException("Fichier inaccessible");
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if(out.size()+n>20_000_000)throw new IOException("Fichier trop volumineux (20 Mo maximum)");out.write(buffer,0,n);}
        return out.toString(StandardCharsets.UTF_8.name());
    }
}
