package com.Project258;
/**
 * Created by VISHNU PRASANTH on 4/5/2016.
 */
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileExplorer extends Activity
{
	private List<String> fileList = new ArrayList<String>();
	private ListView listView;
	private Intent selectedFileIntent;
	private File root;
	private File selected;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.explorer);

		selectedFileIntent = new Intent();

		// list view for listing all the files and directory in the root directory ////
		listView = (ListView) findViewById(R.id.listView);
		listView.setOnItemClickListener(new OnItemClickListener()
		{
			public void onItemClick(AdapterView<?> listView, View v, int position, long id)
			{				
				selected = new File(fileList.get(position));

				/// list the contents inside if the selected item is a directory ///
				if(selected.isDirectory()) 
				{
					listDir();
				}
				else 
				{
					finishSelection();
				}				
			}
		});		
		registerForContextMenu(listView);

		root = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
		selected = root;
		listDir(); 
	}		

	/// this method will send the file path using putExtra where DriveActivity will receive the packaged data ///
	private void finishSelection() 
	{
		selectedFileIntent.putExtra("file", selected);
		setResult(RESULT_OK, selectedFileIntent);
		finish();
	}

	/// method to list the contents inside the directory ////
	private void listDir() 
	{
		File[] files = selected.listFiles();

		fileList.clear();
		for (File file : files)
		{
			fileList.add(file.getPath()); 
		}

		if(fileList.isEmpty()){
			TextView emptyText = (TextView)findViewById(android.R.id.empty);
			listView.setEmptyView(emptyText);
		}
		ArrayAdapter<String> directoryList = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_1, fileList);
		listView.setAdapter(directoryList);
	} 

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{		   
		if (v.getId() == R.id.listView) 
		{
			menu.setHeaderTitle("Select Folder?");
			String[] menuItems = new String[] { "Yes", "No" };

			for (int i = 0; i < menuItems.length; i++) 
			{
				menu.add(Menu.NONE, i, i, menuItems[i]);
			}			   			   			   
		}
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		String selectedString = item.getTitle().toString();
		
		if (selectedString == "Yes")
		{
			AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
			String path = fileList.get(info.position);
			selected = new File(path);
			
			finishSelection();		
		}		
		return true;				
	}
	
	@Override
    public void onBackPressed() 
    {
		if (!selected.getAbsolutePath().equals(root.getAbsolutePath()))
		{
			selected = selected.getParentFile();
			listDir();
			return;
		}
    	super.onBackPressed();
    }	
}
