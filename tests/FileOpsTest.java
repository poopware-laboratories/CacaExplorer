// SPDX-License-Identifier: AGPL-3.0-or-later
import org.poopware.cacaexplorer.FileOps;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;

public class FileOpsTest {
    interface Attempt { void run() throws Exception; }
    static void rejects(Attempt action) throws Exception {
        try { action.run(); } catch (IOException expected) { return; }
        throw new AssertionError("Operation should have failed");
    }
    public static void main(String[] args) throws Exception {
        final File root = Files.createTempDirectory("cacaexplorer-test").toFile();
        try {
            final File source = new File(root,"source"); source.mkdir();
            byte[] data = new byte[180001]; for(int i=0;i<data.length;i++)data[i]=(byte)i;
            Files.write(new File(source,"binary.dat").toPath(),data);
            new File(source,"empty").mkdir();
            File copy = new File(root,"copy"); FileOps.copy(source,copy);
            if(!Arrays.equals(data,Files.readAllBytes(new File(copy,"binary.dat").toPath())))throw new AssertionError("Binary copy corrupted");
            if(!new File(copy,"empty").isDirectory())throw new AssertionError("Empty folder lost");
            rejects(new Attempt(){public void run()throws Exception{FileOps.copy(source,new File(source,"nested"));}});
            rejects(new Attempt(){public void run()throws Exception{FileOps.copy(source,new File(root,"copy"));}});
            rejects(new Attempt(){public void run()throws Exception{FileOps.named(root,"../escape");}});
            rejects(new Attempt(){public void run()throws Exception{FileOps.named(root,"");}});
            rejects(new Attempt(){public void run()throws Exception{FileOps.move(source,new File(root,"copy"));}});
            File moved=new File(root,"moved");FileOps.move(copy,moved);
            if(copy.exists()||!new File(moved,"binary.dat").exists())throw new AssertionError("Move failed");
            final File link=new File(root,"link");Files.createSymbolicLink(link.toPath(),source.toPath());
            rejects(new Attempt(){public void run()throws Exception{FileOps.delete(link);}});
            rejects(new Attempt(){public void run()throws Exception{FileOps.copy(link,new File(root,"linked-copy"));}});
            Files.delete(link.toPath());
            FileOps.delete(moved);if(moved.exists())throw new AssertionError("Recursive delete failed");
            System.out.println("PASS: binary and recursive copy, empty directories, no overwrite, descendant rejection, name validation, move, symlink protection, recursive delete");
        } finally { FileOps.delete(root); }
    }
}
