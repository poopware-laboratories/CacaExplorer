// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.*;
import android.widget.*;
import org.json.JSONArray;
import java.io.*;
import java.util.*;

/** Tablet navigation, selection and secondary tools; filesystem work stays off the UI thread. */
final class ExplorerFeatures {
    final MainActivity a;
    final Set<File> selected=new LinkedHashSet<File>();
    final ArrayList<File> tabs=new ArrayList<File>(), bookmarks=new ArrayList<File>();
    boolean selecting, searching, recents;
    int active, sort;
    private File[] batch;
    private boolean moveBatch;
    private LinearLayout tabBar;
    ExplorerFeatures(MainActivity activity){a=activity;sort=a.getPreferences(0).getInt("sort",0);}
    void restore(File current){
        load("tabs",tabs);load("bookmarks",bookmarks);
        if(tabs.isEmpty())tabs.add(current);
        active=Math.min(a.getPreferences(0).getInt("tab",0),tabs.size()-1);
        a.current=tabs.get(active);
    }
    private void load(String key,ArrayList<File> files){try{JSONArray data=new JSONArray(a.getPreferences(0).getString(key,"[]"));for(int i=0;i<data.length()&&i<32;i++)files.add(new File(data.getString(i)));}catch(Exception ignored){}}
    private String encode(ArrayList<File> files){JSONArray data=new JSONArray();for(File f:files)data.put(f.getPath());return data.toString();}
    void save(){a.getPreferences(0).edit().putString("tabs",encode(tabs)).putInt("tab",active).putString("bookmarks",encode(bookmarks)).putInt("sort",sort).commit();}
    View makeTabs(){HorizontalScrollView scroll=new HorizontalScrollView(a);tabBar=new LinearLayout(a);scroll.addView(tabBar);renderTabs();return scroll;}
    private void renderTabs(){
        if(tabBar==null)return;tabBar.removeAllViews();
        for(int i=0;i<tabs.size();i++){final int index=i;Button b=new Button(a);File f=tabs.get(i);b.setText((i==active?"> ":"")+(f.getName().length()==0?"/":f.getName()));b.setTypeface(Typeface.DEFAULT);a.styleButton(b,i==active);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(a.busy)return;active=index;searching=false;clearSelection();a.navigate(tabs.get(index));}});b.setOnLongClickListener(new View.OnLongClickListener(){public boolean onLongClick(View v){if(a.busy||tabs.size()==1)return true;tabs.remove(index);active=Math.min(active,tabs.size()-1);a.navigate(tabs.get(active));renderTabs();return true;}});tabBar.addView(b);}
        Button add=new Button(a);a.styleButton(add,false);add.setText("+");add.setContentDescription("New tab");add.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(a.busy)return;if(tabs.size()>=12){a.error("Up to 12 tabs are supported.");return;}tabs.add(a.current);active=tabs.size()-1;renderTabs();}});tabBar.addView(add);
    }
    void updateTab(File f){tabs.set(active,f);clearSelection();renderTabs();}
    Comparator<File> comparator(){final int order=sort;return new Comparator<File>(){public int compare(File x,File y){if(x.isDirectory()!=y.isDirectory())return x.isDirectory()?-1:1;long delta=0;if(order==1)delta=y.lastModified()-x.lastModified();if(order==2)delta=y.length()-x.length();if(delta!=0)return delta<0?-1:1;if(order==3){int c=extension(x).compareToIgnoreCase(extension(y));if(c!=0)return c;}return x.getName().compareToIgnoreCase(y.getName());}};}
    private static String extension(File f){String n=f.getName();int i=n.lastIndexOf('.');return i<0?"":n.substring(i+1);}
    File[] search(File root,String query){
        if(query.length()==0&&!recents)return root.listFiles();
        final int generation=a.listing;
        ArrayList<File> result=new ArrayList<File>();ArrayDeque<File> queue=new ArrayDeque<File>();HashSet<String> visited=new HashSet<String>();queue.add(root);int scanned=0;
        while(generation==a.listing&&!queue.isEmpty()&&scanned++<20000&&result.size()<1000){File dir=queue.remove();try{if(!visited.add(dir.getCanonicalPath()))continue;}catch(IOException e){continue;}
            File[] files=dir.listFiles();if(files==null)continue;
            for(File f:files){if(!a.hidden&&f.getName().startsWith("."))continue;if((!recents||f.isFile())&&f.getName().toLowerCase(Locale.US).contains(query)){result.add(f);if(result.size()==1000)break;}if(f.isDirectory())try{if(new File(f.getParentFile().getCanonicalFile(),f.getName()).equals(f.getCanonicalFile()))queue.add(f);}catch(IOException ignored){}}
        }
        return result.toArray(new File[result.size()]);
    }
    void toggle(File f){if(!selected.remove(f))selected.add(f);a.updateSelectionControls();a.status.setText(selected.size()+" selected · Tap Copy or Move above");}
    void clearSelection(){selecting=false;selected.clear();a.updateSelectionControls();}
    void copySelection(boolean move){if(selected.isEmpty()){a.error("Select at least one item.");return;}stage(roots(selected),move);clearSelection();a.list.invalidateViews();a.status.setText("Choose a destination folder, then Paste in the toolbar");}
    void selectionMenu(){
        if(!selecting){selecting=true;a.updateSelectionControls();a.status.setText("Tap checkboxes, then Copy or Move above");a.list.invalidateViews();return;}
        new AlertDialog.Builder(a).setTitle(selected.size()+" selected").setItems(new String[]{"Select all visible","Copy selected","Move selected","Delete selected","ZIP selected","Share selected","Finish selection"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
            if(which==0){for(int i=0;i<a.list.getAdapter().getCount();i++)selected.add((File)a.list.getAdapter().getItem(i));a.list.invalidateViews();a.status.setText(selected.size()+" selected");return;}
            if(which==6){clearSelection();a.list.invalidateViews();return;}
            if(selected.isEmpty()){a.error("Select at least one item.");return;}
            final File[] files=roots(selected);
            if(which==1||which==2){stage(files,which==2);clearSelection();a.list.invalidateViews();a.status.setText(files.length+" items ready · Choose destination, then Paste");}
            if(which==3)new AlertDialog.Builder(a).setTitle("Delete "+files.length+" items?").setMessage("Permanently deletes selected items and their contents. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){a.job("Deleting selection…",new MainActivity.Work(){public void run()throws Exception{for(File f:files)FileOps.delete(f);}},new Runnable(){public void run(){clearSelection();}});}}).show();
            if(which==4)zip(files);if(which==5)share(files);
        }}).show();
    }
    // Recursive search can select both a directory and its children; operate on roots once.
    private File[] roots(Set<File> set){ArrayList<File> result=new ArrayList<File>();for(File f:set){boolean nested=false;for(File parent=f.getParentFile();parent!=null;parent=parent.getParentFile())if(set.contains(parent)){nested=true;break;}if(!nested)result.add(f);}return result.toArray(new File[result.size()]);}
    boolean hasBatch(){return batch!=null;}
    void clearBatch(){batch=null;a.getPreferences(0).edit().remove("clipboard").commit();}
    void stage(File[] files,boolean move){batch=files;moveBatch=move;JSONArray paths=new JSONArray();for(File f:files)paths.put(f.getPath());a.getPreferences(0).edit().putString("clipboard",paths.toString()).putBoolean("clipboardMove",move).commit();a.updateClipboard();}
    void restoreClipboard(){try{JSONArray paths=new JSONArray(a.getPreferences(0).getString("clipboard","[]"));if(paths.length()==0)return;batch=new File[paths.length()];for(int i=0;i<batch.length;i++)batch[i]=new File(paths.getString(i));moveBatch=a.getPreferences(0).getBoolean("clipboardMove",false);}catch(Exception e){clearBatch();}}
    int clipboardCount(){return batch==null?0:batch.length;}
    String clipboardDescription(){return batch==null?"":(moveBatch?"Move: ":"Copy: ")+(batch.length==1?batch[0].getName():batch.length+" items");}
    void pasteBatch(){final File[] sources=batch;final File parent=a.current;final boolean moving=moveBatch;
        a.job(moving?"Moving selection…":"Copying selection…",new MainActivity.Work(){public void run()throws Exception{
            HashSet<String> names=new HashSet<String>();for(File f:sources){if(!f.exists())throw new IOException("Source no longer exists: "+f);if(!names.add(f.getName())||new File(parent,f.getName()).exists())throw new IOException("Destination name conflict: "+f.getName());}
            int done=0;try{for(File f:sources){File to=new File(parent,f.getName());if(moving)FileOps.move(f,to);else FileOps.copy(f,to);done++;}}catch(IOException e){throw new IOException(done+" of "+sources.length+" items completed. "+e.getMessage());}
        }},new Runnable(){public void run(){if(moving)clearBatch();a.updateClipboard();clearSelection();}});
    }
    void fileAction(final File f,int which){
        if(which==6){selecting=true;toggle(f);a.list.invalidateViews();}
        if(which==7)preview(f);
        if(which==8)share(new File[]{f});
        if(which==9)zip(new File[]{f});
        if(which==10){final File parent=f.getParentFile();a.prompt("Extract ZIP into new folder",f.getName()+"-extracted",new MainActivity.NameAction(){public void run(String name)throws Exception{final File to=FileOps.named(parent,name);a.job("Extracting ZIP…",new MainActivity.Work(){public void run()throws Exception{ArchiveOps.extract(f,to);}});}});}
        if(which==11){if(!f.isDirectory()){a.error("Choose a folder to bookmark.");return;}if(!bookmarks.contains(f))bookmarks.add(f);save();a.status.setText("Bookmarked "+f.getName());}
    }
    private void zip(final File[] files){final File parent=a.current;a.prompt("Create ZIP","Archive.zip",new MainActivity.NameAction(){public void run(String name)throws Exception{final File to=FileOps.named(parent,name);a.job("Creating ZIP…",new MainActivity.Work(){public void run()throws Exception{ArchiveOps.zip(files,to);}});}});}
    private void preview(final File f){
        final String[] contents=new String[1];a.job("Reading preview…",new MainActivity.Work(){public void run()throws Exception{
            if(!f.isFile())throw new IOException("Choose a text file.");InputStream in=new FileInputStream(f);ByteArrayOutputStream out=new ByteArrayOutputStream();
            try{byte[] b=new byte[4096];int n;while(out.size()<262144&&(n=in.read(b,0,Math.min(b.length,262144-out.size())))!=-1)out.write(b,0,n);}finally{in.close();}
            contents[0]=new String(out.toByteArray(),"UTF-8")+(f.length()>262144?"\n\n[Preview limited to 256 KiB]":"");
        }},new Runnable(){public void run(){TextView text=new TextView(a);text.setText(contents[0]);text.setTypeface(Typeface.MONOSPACE);text.setTextSize(14);text.setTextIsSelectable(true);text.setPadding(16,16,16,16);ScrollView scroll=new ScrollView(a);scroll.addView(text);new AlertDialog.Builder(a).setTitle(f.getName()+" · UTF-8 preview").setView(scroll).setPositiveButton("Close",null).show();}});
    }
    private void share(File[] files){ArrayList<android.os.Parcelable> uris=new ArrayList<android.os.Parcelable>();for(File f:files){if(!f.isFile()){a.error("Sharing folders requires creating a ZIP first.");return;}uris.add(Uri.fromFile(f));}Intent intent=new Intent(uris.size()==1?Intent.ACTION_SEND:Intent.ACTION_SEND_MULTIPLE);intent.setType("*/*");if(uris.size()==1)intent.putExtra(Intent.EXTRA_STREAM,uris.get(0));else intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM,uris);try{a.startActivity(Intent.createChooser(intent,"Share files"));}catch(ActivityNotFoundException e){a.error("No sharing app is installed.");}}
    void more(){
        new AlertDialog.Builder(a).setTitle("CacaExplorer").setItems(new String[]{"Refresh","Search subfolders","Sort by…",a.hidden?"Hide hidden files":"Show hidden files","Go to folder","Bookmarks","Bookmark this folder","Theme","Storage","About"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){
            if(w==0)a.refresh();if(w==1)a.beginSearch();if(w==2)new AlertDialog.Builder(a).setTitle("Sort · folders first").setSingleChoiceItems(new String[]{"Name A–Z","Newest first","Largest first","File type"},sort,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int i){sort=i;save();d.dismiss();a.refresh();}}).show();
            if(w==3)a.toggleHidden();if(w==4)a.prompt("Go to folder",a.current.getPath(),new MainActivity.NameAction(){public void run(String value){a.navigate(new File(value));}});
            if(w==5)showBookmarks();if(w==6){if(!bookmarks.contains(a.current))bookmarks.add(a.current);save();a.status.setText("Folder bookmarked");}
            if(w==7)new AlertDialog.Builder(a).setTitle("Theme · Poopterm palettes").setItems(ThemeCatalog.NAMES,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int i){final String name=ThemeCatalog.NAMES[i];if(name.equals("Custom")){a.prompt("Background, text, panel, accent (#RRGGBB)",a.getPreferences(0).getString("custom","#232325,#eeeeef,#2b2b2e,#29a7ee"),new MainActivity.NameAction(){public void run(String value)throws Exception{String[] parts=value.split(",");if(parts.length!=4)throw new IOException("Enter four comma-separated colors.");int[] colors=new int[4];for(int j=0;j<4;j++){parts[j]=parts[j].trim();if(!parts[j].matches("#[0-9a-fA-F]{6}"))throw new IOException("Use #RRGGBB colors.");colors[j]=android.graphics.Color.parseColor(parts[j]);}if(ThemeCatalog.contrast(colors[0],colors[1])<4.5)throw new IOException("Text needs more contrast against the background.");a.getPreferences(0).edit().putString("custom",android.text.TextUtils.join(",",parts)).commit();setTheme(name);}});}else setTheme(name);}}).show();
            if(w==8){File storage=Environment.getExternalStorageDirectory();new AlertDialog.Builder(a).setTitle("Storage").setMessage(storage+"\n\nAvailable: "+(storage.getUsableSpace()/1048576)+" MiB\nTotal: "+(storage.getTotalSpace()/1048576)+" MiB\n\nState: "+Environment.getExternalStorageState()).setPositiveButton("OK",null).show();}
            if(w==9)new AlertDialog.Builder(a).setTitle("CacaExplorer 0.1.1").setMessage("Poopware for Android 3.2 Honeycomb.\n\nPoopterm's chocolate, cream and caramel colors. Native widgets, offline, no root.\n\nTabs: + to add, hold a tab to close.\nFiles: tap to open, hold for actions.\nSelect: tap rows, then Select for batch actions.\nBack exits selection or recursive search.\n\nZIP extraction: 1 GiB / 20,000 entry limit. No trash or undo; deletion is permanent.").setPositiveButton("Nice",null).show();
        }}).show();
    }
    private void setTheme(String name){a.getPreferences(0).edit().putString("theme",name).commit();save();a.recreate();}
    void showBookmarks(){if(bookmarks.isEmpty()){a.error("No bookmarks yet. Use Bookmark this folder in More.");return;}final String[] names=new String[bookmarks.size()];for(int i=0;i<names.length;i++)names[i]=bookmarks.get(i).getPath();new AlertDialog.Builder(a).setTitle("Bookmarks").setItems(names,new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,final int i){new AlertDialog.Builder(a).setTitle(names[i]).setItems(new String[]{"Open","Remove bookmark"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){if(w==0)a.navigate(bookmarks.get(i));else{bookmarks.remove(i);save();}}}).show();}}).show();}
}
