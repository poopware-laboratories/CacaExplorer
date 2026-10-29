// SPDX-License-Identifier: AGPL-3.0-or-later
import org.poopware.cacaexplorer.*;
import java.io.*;
import java.nio.file.*;
import java.util.zip.*;
import java.util.Arrays;
public class ArchiveOpsTest {
    static void malicious(File zip,String entry)throws Exception {
        ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip));
        out.putNextEntry(new ZipEntry(entry));out.write(new byte[]{1,2,3});out.closeEntry();out.close();
    }
    static void rejects(File zip,File target)throws Exception {
        try { ArchiveOps.extract(zip,target); } catch(IOException expected) {
            if(target.exists())throw new AssertionError("Partial extraction published");return;
        }
        throw new AssertionError("Unsafe archive accepted");
    }
    public static void main(String[] args)throws Exception {
        File root=Files.createTempDirectory("caca-zip-tests").toFile();
        try {
            File source=new File(root,"source");source.mkdir();new File(source,"empty").mkdir();
            byte[] data=new byte[130003];for(int i=0;i<data.length;i++)data[i]=(byte)i;
            Files.write(new File(source,"data.bin").toPath(),data);
            File zip=new File(root,"archive.zip");ArchiveOps.zip(new File[]{source},zip);
            File extracted=new File(root,"extracted");ArchiveOps.extract(zip,extracted);
            if(!Arrays.equals(data,Files.readAllBytes(new File(extracted,"source/data.bin").toPath())))throw new AssertionError("ZIP roundtrip corrupted");
            if(!new File(extracted,"source/empty").isDirectory())throw new AssertionError("Empty directory lost");
            try{ArchiveOps.extract(zip,extracted);throw new AssertionError("Overwrite allowed");}catch(IOException expected){}
            File bad=new File(root,"bad.zip");malicious(bad,"../escape");rejects(bad,new File(root,"bad-output"));
            if(new File(root,"escape").exists())throw new AssertionError("Traversal escaped");
            malicious(bad,"/absolute");rejects(bad,new File(root,"absolute-output"));
            malicious(bad,"..\\escape");rejects(bad,new File(root,"backslash-output"));
            Files.write(bad.toPath(),new byte[]{1,2,3});rejects(bad,new File(root,"invalid-output"));
            byte[] archive=Files.readAllBytes(zip.toPath());Files.write(bad.toPath(),Arrays.copyOf(archive,archive.length/2));rejects(bad,new File(root,"truncated-output"));
            for(File f:root.listFiles())if(f.getName().startsWith(".caca"))throw new AssertionError("Temporary output leaked");
            System.out.println("PASS: ZIP binary roundtrip, empty directories, overwrite refusal, traversal, absolute paths, backslashes, invalid and truncated archives, failure cleanup");
        } finally {FileOps.delete(root);}
    }
}
