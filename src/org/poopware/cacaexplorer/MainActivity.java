// SPDX-License-Identifier: AGPL-3.0-or-later
package org.poopware.cacaexplorer;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.*;
import android.view.*;
import android.widget.*;
import android.webkit.MimeTypeMap;
import java.io.*;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    private int BG=0xff232325, PANEL=0xff2b2b2e, FG=0xffeeeeef, GOLD=0xff29a7ee;
    private boolean grid;
    File current, clipboard;
    ExplorerFeatures features;
    boolean cut, hidden, busy;
    private TextView path;
    private LinearLayout breadcrumbs;
    private final ArrayList<File> history=new ArrayList<File>();
    private int historyIndex=-1;
    private boolean navigatingHistory;
    TextView status;
    private EditText filter;
    AbsListView list;
    Button paste;
    private TextView clipboardBanner;
    private LinearLayout selectionBar;
    private TextView selectionCount;
    private Button copySelected,moveSelected;
    private final ArrayList<File> entries = new ArrayList<File>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler();
    volatile int listing = 0;

    @Override public void onCreate(Bundle state) {
        String theme=getPreferences(0).getString("theme","Graphite");
        int[] colors=ThemeCatalog.find(theme);
        if(theme.equals("Custom")){try{String[] values=getPreferences(0).getString("custom","#232325,#eeeeef,#2b2b2e,#29a7ee").split(",");colors[1]=android.graphics.Color.parseColor(values[0]);colors[0]=android.graphics.Color.parseColor(values[1]);colors[2]=android.graphics.Color.parseColor(values[2]);colors[7]=android.graphics.Color.parseColor(values[3]);}catch(Exception ignored){}}
        BG=colors[1];FG=colors[0];PANEL=colors[2];GOLD=colors[7];
        if(ThemeCatalog.contrast(GOLD,BG)<3)GOLD=ThemeCatalog.contrast(colors[9],BG)>=3?colors[9]:FG;
        setTheme(ThemeCatalog.light(BG)?android.R.style.Theme_Holo_Light_NoActionBar:android.R.style.Theme_Holo_NoActionBar);
        super.onCreate(state);
        grid=getPreferences(0).getBoolean("grid",true);
        features=new ExplorerFeatures(this);
        hidden = getPreferences(0).getBoolean("hidden", false);
        String saved = state == null ? getPreferences(0).getString("path", Environment.getExternalStorageDirectory().getPath()) : state.getString("path");
        current = new File(saved);
        features.restore(current);
        LinearLayout root = new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(BG);root.setFocusableInTouchMode(true);root.requestFocus();
        LinearLayout header=new LinearLayout(this);header.setPadding(dp(12),dp(6),dp(12),dp(6));header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=text("CacaExplorer",20,FG);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        TextView version=text("Honeycomb edition",12,GOLD);version.setTypeface(Typeface.DEFAULT);header.addView(version);root.addView(header);
        LinearLayout bar = new LinearLayout(this);
        button(bar,"Back",new Runnable(){public void run(){history(-1);}});
        button(bar,"Forward",new Runnable(){public void run(){history(1);}});
        button(bar,"Up",new Runnable(){public void run(){up();}});
        button(bar,"New folder",new Runnable(){public void run(){newFolder();}});
        button(bar,grid?"List view":"Icon view",new Runnable(){public void run(){getPreferences(0).edit().putBoolean("grid",!grid).commit();features.save();recreate();}});
        paste=button(bar,"Paste",new Runnable(){public void run(){paste();}});
        button(bar,"Menu",new Runnable(){public void run(){more();}});
        root.addView(bar);
        clipboardBanner=text("",14,GOLD);clipboardBanner.setPadding(dp(14),dp(10),dp(14),dp(10));clipboardBanner.setBackgroundColor(PANEL);clipboardBanner.setVisibility(View.GONE);clipboardBanner.setOnClickListener(new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("Files ready to paste").setItems(new String[]{"Paste into this folder","Clear clipboard","Keep browsing"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){if(w==0)paste();if(w==1){features.clearBatch();updateClipboard();}}}).show();}});root.addView(clipboardBanner);
        selectionBar=new LinearLayout(this);selectionBar.setGravity(Gravity.CENTER_VERTICAL);selectionBar.setBackgroundColor(PANEL);selectionCount=text("0 selected",14,FG);selectionCount.setPadding(dp(12),0,dp(12),0);selectionBar.addView(selectionCount);
        copySelected=button(selectionBar,"Copy",new Runnable(){public void run(){features.copySelection(false);}});
        moveSelected=button(selectionBar,"Move",new Runnable(){public void run(){features.copySelection(true);}});
        button(selectionBar,"Select all",new Runnable(){public void run(){for(File f:entries)features.selected.add(f);updateSelectionControls();list.invalidateViews();}});
        button(selectionBar,"Done",new Runnable(){public void run(){features.clearSelection();list.invalidateViews();}});
        selectionBar.setVisibility(View.GONE);root.addView(selectionBar);
        LinearLayout body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout sidebar=new LinearLayout(this);sidebar.setOrientation(1);sidebar.setPadding(dp(8),dp(12),dp(8),0);sidebar.setBackgroundColor(PANEL);
        TextView places=text("PLACES",12,GOLD);places.setPadding(dp(8),0,0,dp(12));sidebar.addView(places);
        final File home=Environment.getExternalStorageDirectory();
        TextView recents=text("Recent files",16,FG);recents.setPadding(dp(8),dp(14),0,dp(14));recents.setOnClickListener(new View.OnClickListener(){public void onClick(View v){navigate(home);features.recents=true;features.sort=1;refresh();}});sidebar.addView(recents);
        place(sidebar,"Home",home);place(sidebar,"Downloads",new File(home,"Download"));place(sidebar,"Documents",new File(home,"Documents"));place(sidebar,"Pictures",new File(home,"Pictures"));place(sidebar,"Music",new File(home,"Music"));place(sidebar,"Videos",new File(home,"Movies"));place(sidebar,"File system",new File("/"));
        TextView bookmarks=text("Bookmarks",16,FG);bookmarks.setPadding(dp(8),dp(16),0,dp(16));bookmarks.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(!busy)features.showBookmarks();}});sidebar.addView(bookmarks);
        body.addView(sidebar,new LinearLayout.LayoutParams(dp(150),-1));
        LinearLayout content=new LinearLayout(this);content.setOrientation(1);body.addView(content,new LinearLayout.LayoutParams(0,-1,1));
        content.addView(features.makeTabs());
        HorizontalScrollView crumbs=new HorizontalScrollView(this);breadcrumbs=new LinearLayout(this);crumbs.addView(breadcrumbs);content.addView(crumbs);
        path = text("",16,GOLD); path.setPadding(dp(18),dp(10),dp(18),dp(10)); path.setBackgroundColor(PANEL);
        path.setOnClickListener(new View.OnClickListener(){public void onClick(View v){prompt("Go to folder",current.getPath(),new NameAction(){public void run(String value){navigate(new File(value));}});}});
        path.setVisibility(View.GONE);content.addView(path);
        filter = new EditText(this); filter.setSingleLine(true); filter.setTextColor(FG); filter.setTextSize(16); filter.setHint("Filter this folder…"); filter.setHintTextColor(FG); filter.setTypeface(Typeface.DEFAULT);
        content.addView(filter);
        if(grid){GridView icons=new GridView(this);icons.setColumnWidth(dp(132));icons.setNumColumns(GridView.AUTO_FIT);icons.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);icons.setVerticalSpacing(dp(12));icons.setPadding(dp(14),dp(14),dp(14),dp(14));list=icons;}else{ListView details=new ListView(this);details.setDividerHeight(dp(1));list=details;} list.setBackgroundColor(BG); content.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        TextView empty=text("This folder is empty",18,FG);empty.setGravity(Gravity.CENTER);content.addView(empty,new LinearLayout.LayoutParams(-1,0,1));list.setEmptyView(empty);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){public void onItemClick(AdapterView<?> a, View v,int p,long id){File f=entries.get(p); if(features.selecting){features.toggle(f);list.invalidateViews();return;} if(f.isDirectory())navigate(f);else open(f);}});
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){public boolean onItemLongClick(AdapterView<?> a,View v,int p,long id){actions(entries.get(p));return true;}});
        LinearLayout footer = new LinearLayout(this);

        button(footer,"Select",new Runnable(){public void run(){features.selectionMenu();}});
        button(footer,"Search",new Runnable(){public void run(){beginSearch();}});
        button(footer,"Location",new Runnable(){public void run(){path.performClick();}});
        root.addView(footer);
        status = text("Tap to open · Hold for file actions",12,FG); status.setPadding(dp(18),dp(8),dp(10),dp(10)); root.addView(status);
        setContentView(root);
        filter.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){refresh();} public void afterTextChanged(Editable e){}});
        features.restoreClipboard();updateClipboard();history.add(current);historyIndex=0;refresh();
    }
    void styleButton(Button b,boolean active){android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();shape.setColor(active?GOLD:PANEL);shape.setCornerRadius(dp(4));shape.setStroke(dp(1),BG);b.setBackgroundDrawable(shape);b.setTextColor(active?(ThemeCatalog.light(GOLD)?0xff111111:0xffffffff):FG);b.setPadding(dp(8),dp(6),dp(8),dp(6));}
    private void place(LinearLayout parent,String label,final File folder){TextView item=text(label,16,FG);item.setPadding(dp(8),dp(14),dp(4),dp(14));item.setOnClickListener(new View.OnClickListener(){public void onClick(View v){navigate(folder);}});parent.addView(item);}
    private void history(int delta){int target=historyIndex+delta;if(target<0||target>=history.size())return;historyIndex=target;navigatingHistory=true;navigate(history.get(target));navigatingHistory=false;}
    private void renderBreadcrumbs(){breadcrumbs.removeAllViews();ArrayList<File> chain=new ArrayList<File>();for(File f=current;f!=null;f=f.getParentFile())chain.add(0,f);if(features.recents){TextView label=text("Recent files",16,GOLD);label.setPadding(dp(12),dp(12),0,dp(12));breadcrumbs.addView(label);return;}for(final File f:chain){TextView crumb=text((f.getName().length()==0?"/":f.getName())+"  ›  ",15,GOLD);crumb.setPadding(dp(10),dp(12),dp(2),dp(12));crumb.setOnClickListener(new View.OnClickListener(){public void onClick(View v){navigate(f);}});breadcrumbs.addView(crumb);}}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextColor(color);t.setTextSize(size);t.setTypeface(Typeface.DEFAULT);return t;}
    private Button button(LinearLayout parent,String name,final Runnable action){Button b=new Button(this);b.setText(name);styleButton(b,false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT);b.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(!busy)action.run();}});parent.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));return b;}
    void navigate(File dir){if(busy)return;if(!dir.isDirectory()||!dir.canRead()){error("Cannot read folder: "+dir);return;}if(!navigatingHistory){while(history.size()>historyIndex+1)history.remove(history.size()-1);history.add(dir);historyIndex=history.size()-1;}current=dir;features.searching=false;features.recents=false;filter.setHint("Filter this folder…");features.updateTab(dir);filter.setText("");refresh();}
    void beginSearch(){features.searching=true;filter.setHint("Search this folder and subfolders…");filter.setText("");filter.requestFocus();}
    private void up(){File parent=current.getParentFile();if(parent!=null)navigate(parent);}
    @Override public void onBackPressed(){if(busy)return;if(features.selecting){features.clearSelection();list.invalidateViews();return;}if(features.searching){features.searching=false;features.recents=false;filter.setHint("Filter this folder…");filter.setText("");refresh();return;}if(filter.length()>0)filter.setText("");else if(current.getParentFile()!=null)up();else super.onBackPressed();}
    @Override public void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("path",current.getPath());}
    @Override protected void onPause(){super.onPause();features.save();getPreferences(0).edit().putString("path",current.getPath()).putBoolean("hidden",hidden).commit();}
    @Override protected void onDestroy(){listing++;worker.shutdown();super.onDestroy();}
    void refresh(){
        final int generation=++listing; final File folder=current; final boolean showHidden=hidden;
        final String query=filter.getText().toString().toLowerCase(Locale.US); path.setText(folder.getPath());updateClipboard();renderBreadcrumbs();list.setEnabled(false);
        worker.execute(new Runnable(){public void run(){
            if(generation!=listing)return;
            File[] files=(features.searching||features.recents)?features.search(folder,query):folder.listFiles(); final boolean readable=files!=null;
            final ArrayList<File> found=new ArrayList<File>();
            if(files!=null)for(File f:files)if((showHidden||!f.getName().startsWith("."))&&f.getName().toLowerCase(Locale.US).contains(query))found.add(f);
            Collections.sort(found,features.comparator());
            handler.post(new Runnable(){public void run(){if(generation!=listing||isFinishing())return;entries.clear();entries.addAll(found);list.setEnabled(!busy);list.setAdapter(new FileAdapter());status.setText(!readable?"Cannot read this folder":entries.size()+(features.recents?" recent files":" items")+(features.searching?" · Recursive search (up to 1,000 results)":" · Hold a row for actions"));}});
        }});
    }
    private class FileAdapter extends BaseAdapter {
        public int getCount(){return entries.size();}public Object getItem(int p){return entries.get(p);}public long getItemId(int p){return p;}
        public View getView(int p,View recycled,android.view.ViewGroup parent){
            LinearLayout row;
            if(recycled==null){row=new LinearLayout(MainActivity.this);row.setOrientation(grid?1:0);row.setGravity(grid?Gravity.CENTER:Gravity.CENTER_VERTICAL);row.setPadding(dp(14),dp(10),dp(14),dp(10));CheckBox check=new CheckBox(MainActivity.this);check.setFocusable(false);check.setClickable(false);row.addView(check);ImageView icon=new ImageView(MainActivity.this);row.addView(icon,new LinearLayout.LayoutParams(dp(grid?58:34),dp(grid?58:34)));LinearLayout labels=new LinearLayout(MainActivity.this);labels.setOrientation(1);labels.setPadding(dp(grid?2:12),dp(grid?6:0),0,0);labels.addView(text("",17,FG));labels.addView(text("",12,FG));row.addView(labels,grid?new LinearLayout.LayoutParams(-1,dp(60)):new LinearLayout.LayoutParams(0,-2,1));}else row=(LinearLayout)recycled;
            File f=entries.get(p);CheckBox check=(CheckBox)row.getChildAt(0);check.setVisibility(features.selecting?View.VISIBLE:View.GONE);check.setChecked(features.selected.contains(f));((ImageView)row.getChildAt(1)).setImageResource(f.isDirectory()?R.drawable.folder:R.drawable.document);LinearLayout labels=(LinearLayout)row.getChildAt(2);TextView name=(TextView)labels.getChildAt(0);name.setText(f.getName());name.setTextColor(FG);name.setGravity(grid?Gravity.CENTER:Gravity.LEFT);name.setMaxLines(grid?2:1);name.setEllipsize(TextUtils.TruncateAt.END);((TextView)labels.getChildAt(1)).setVisibility(grid?View.GONE:View.VISIBLE);row.setBackgroundColor(features.selected.contains(f)?PANEL:BG);
            ((TextView)labels.getChildAt(1)).setText((f.isDirectory()?"Folder":size(f.length()))+"   ·   "+DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(f.lastModified())));return row;
        }
    }
    private String size(long bytes){if(bytes<1024)return bytes+" B";if(bytes<1048576)return String.format(Locale.US,"%.1f KiB",bytes/1024.0);return String.format(Locale.US,"%.1f MiB",bytes/1048576.0);}
    void open(File f){
        String name=f.getName();int dot=name.lastIndexOf('.');String ext=dot<0?"":name.substring(dot+1).toLowerCase(Locale.US);
        String mime=ext.equals("apk")?"application/vnd.android.package-archive":MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        Intent intent=new Intent(Intent.ACTION_VIEW);intent.setDataAndType(Uri.fromFile(f),mime==null?"*/*":mime);
        try{startActivity(intent);}catch(ActivityNotFoundException e){error("No installed app can open this file.");}catch(SecurityException e){error(e.getMessage());}
    }
    private void actions(final File f){
        if(busy)return;
        new AlertDialog.Builder(this).setTitle(f.getName()).setItems(new String[]{"Open","Copy","Move","Rename","Delete","Properties","Select","Preview text","Share","Create ZIP","Extract ZIP","Bookmark folder"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
            if(which==0){if(f.isDirectory())navigate(f);else open(f);}
            if(which==1||which==2){features.stage(new File[]{f},which==2);}
            if(which==3)prompt("Rename",f.getName(),new NameAction(){public void run(String name)throws Exception{final File dest=FileOps.named(f.getParentFile(),name);job("Renaming…",new Work(){public void run()throws Exception{FileOps.move(f,dest);}});}});
            if(which==4)new AlertDialog.Builder(MainActivity.this).setTitle("Delete "+f.getName()+"?").setMessage("Permanently deletes this item and any folder contents. This cannot be undone.").setNegativeButton("Cancel",null).setPositiveButton("Delete",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){job("Deleting…",new Work(){public void run()throws Exception{FileOps.delete(f);}});}}).show();
            if(which>=6){features.fileAction(f,which);return;}
            if(which==5)new AlertDialog.Builder(MainActivity.this).setTitle("Properties").setMessage(f.getPath()+"\n\n"+(f.isDirectory()?"Folder":size(f.length()))+"\nReadable: "+f.canRead()+"\nWritable: "+f.canWrite()+"\nModified: "+new Date(f.lastModified())).setPositiveButton("OK",null).show();
        }}).show();
    }
    void updateSelectionControls(){if(selectionBar==null)return;selectionBar.setVisibility(features.selecting?View.VISIBLE:View.GONE);selectionCount.setText(features.selected.size()+" selected");copySelected.setEnabled(!features.selected.isEmpty());moveSelected.setEnabled(!features.selected.isEmpty());}
    void updateClipboard(){if(paste==null||clipboardBanner==null)return;int count=features.clipboardCount();paste.setText(count>0?"Paste ("+count+")":"Paste");styleButton(paste,count>0);paste.setEnabled(!busy);clipboardBanner.setVisibility(count>0?View.VISIBLE:View.GONE);clipboardBanner.setText(features.clipboardDescription()+"  →  "+current.getPath()+"\nBrowse to a destination, then tap Paste. Tap this bar to clear.");}
    private void paste(){if(features.hasBatch()){if(features.recents){error("Open a destination folder before pasting.");return;}features.pasteBatch();return;}new AlertDialog.Builder(this).setTitle("Paste files").setMessage("Hold a file and choose Copy or Move, open the destination folder, then tap Paste in the toolbar. For multiple files, use Select.").setPositiveButton("OK",null).show();}
    private void newFolder(){final File parent=current;prompt("New folder","",new NameAction(){public void run(String name)throws Exception{final File f=FileOps.named(parent,name);job("Creating folder…",new Work(){public void run()throws Exception{if(!f.mkdir())throw new IOException("Cannot create folder; check the name, permissions, or existing files.");}});}});}
    private void more(){features.more();}
    void toggleHidden(){hidden=!hidden;refresh();}
    interface NameAction{void run(String name)throws Exception;}
    void prompt(String title,String initial,final NameAction action){final EditText input=new EditText(this);input.setSingleLine(true);input.setText(initial);input.selectAll();final AlertDialog dialog=new AlertDialog.Builder(this).setTitle(title).setView(input).setNegativeButton("Cancel",null).setPositiveButton("OK",null).create();dialog.setOnShowListener(new DialogInterface.OnShowListener(){public void onShow(DialogInterface d){dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{action.run(input.getText().toString());dialog.dismiss();}catch(Exception e){input.setError(e.getMessage());}}});}});dialog.show();}
    interface Work{void run()throws Exception;}
    void job(String message,Work work){job(message,work,null);}
    void job(String message,final Work work,final Runnable done){if(busy)return;busy=true;listing++;list.setEnabled(false);filter.setEnabled(false);final ProgressDialog progress=ProgressDialog.show(this,"CacaExplorer",message,true,false);worker.execute(new Runnable(){public void run(){String failure=null;try{work.run();}catch(Exception e){failure=e.getMessage();}final String problem=failure;handler.post(new Runnable(){public void run(){busy=false;if(isFinishing())return;progress.dismiss();list.setEnabled(true);filter.setEnabled(true);if(problem!=null)error(problem);else if(done!=null)done.run();refresh();}});}});}
    void error(String message){new AlertDialog.Builder(this).setTitle("Could not complete action").setMessage(message==null?"Unknown error":message).setPositiveButton("OK",null).show();}
}
