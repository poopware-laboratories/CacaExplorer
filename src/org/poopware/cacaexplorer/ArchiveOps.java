// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** ZIP operations publish only complete output and never replace existing files. */
public final class ArchiveOps {
    private static final long MAX_BYTES = 1024L * 1024 * 1024;
    private static final int MAX_ENTRIES = 20000;
    private static void transfer(InputStream in, OutputStream out) throws IOException {
        byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
    }
    public static void zip(File[] sources,File target)throws IOException {
        if(target.exists())throw new IOException("Archive already exists.");
        for(File f:sources){String path=f.getCanonicalPath();if(target.getCanonicalPath().startsWith(path+File.separator))throw new IOException("Archive cannot be inside its source folder.");}
        File temp=File.createTempFile(".caca-", ".zip", target.getParentFile());
        try{
            ZipOutputStream out=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(temp)));
            try{Set<String> names=new HashSet<String>();for(File f:sources)add(out,f,f.getName(),names);}finally{out.close();}
            if(target.exists()||!temp.renameTo(target))throw new IOException("Cannot publish archive.");
        }finally{temp.delete();}
    }
    private static void add(ZipOutputStream out,File file,String name,Set<String> names)throws IOException {
        File expected=new File(file.getParentFile().getCanonicalFile(),file.getName());
        if(!expected.equals(file.getCanonicalFile()))throw new IOException("Symbolic links are not supported.");
        if(!names.add(name)||names.size()>MAX_ENTRIES)throw new IOException("Duplicate archive name or too many entries.");
        if(file.isDirectory()){
            out.putNextEntry(new ZipEntry(name+"/"));out.closeEntry();File[] children=file.listFiles();
            if(children==null)throw new IOException("Cannot read "+file);
            for(File f:children)add(out,f,name+"/"+f.getName(),names);
        }else{
            if(!file.isFile())throw new IOException("Not a regular file: "+file);
            out.putNextEntry(new ZipEntry(name));InputStream in=new FileInputStream(file);
            try{transfer(in,out);}finally{in.close();}out.closeEntry();
        }
    }
    public static void extract(File zip,File destination)throws IOException {
        if(destination.exists())throw new IOException("Extraction folder already exists.");
        File temp=File.createTempFile(".caca-extract-", "",destination.getParentFile());
        if(!temp.delete()||!temp.mkdir())throw new IOException("Cannot create extraction folder.");
        boolean complete=false;
        try{
            String prefix=temp.getCanonicalPath()+File.separator;
            ZipInputStream in=new ZipInputStream(new BufferedInputStream(new FileInputStream(zip)));
            try{
                ZipEntry entry;byte[] buffer=new byte[65536];long total=0;int count=0;
                Set<String> names=new HashSet<String>();
                while((entry=in.getNextEntry())!=null){
                    if(++count>MAX_ENTRIES)throw new IOException("Archive has too many entries.");
                    String name=entry.getName();File file=new File(temp,name);
                    if(name.startsWith("/")||name.indexOf('\\')>=0||!file.getCanonicalPath().startsWith(prefix))throw new IOException("Unsafe archive path: "+name);
                    if(!names.add(file.getCanonicalPath()))throw new IOException("Duplicate archive entry: "+name);
                    if(entry.isDirectory()){if(!file.mkdirs()&&!file.isDirectory())throw new IOException("Cannot create folder.");}
                    else{
                        File parent=file.getParentFile();if(!parent.mkdirs()&&!parent.isDirectory())throw new IOException("Cannot create folder.");
                        if(!file.createNewFile())throw new IOException("Archive entry already exists.");
                        OutputStream out=new FileOutputStream(file);
                        try{int n;while((n=in.read(buffer))!=-1){total+=n;if(total>MAX_BYTES)throw new IOException("Archive exceeds 1 GiB extraction limit.");out.write(buffer,0,n);}}finally{out.close();}
                    }
                    in.closeEntry();
                }
            }finally{in.close();}
            // ZipInputStream accepts arbitrary non-ZIP data as an empty archive.
            ZipFile checked=new ZipFile(zip);checked.close();
            if(destination.exists()||!temp.renameTo(destination))throw new IOException("Cannot publish extracted folder.");
            complete=true;
        }finally{if(!complete)FileOps.delete(temp);}
    }
}
