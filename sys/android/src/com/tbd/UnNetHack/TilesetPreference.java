package com.tbd.UnNetHack;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

public class TilesetPreference extends Preference implements PreferenceManager.OnActivityResultListener
{
	private static final int GET_IMAGE_REQUEST = 342;

	private String[] mEntries;
	private String[] mEntryValues;
	private TextView mTilesetPath;
	private EditText mTileW;
	private EditText mTileH;
	private ViewGroup mTilesetUI;
	private LinearLayout mRoot;
	private Settings mSettings;
	private String mCustomTilesetPath;
	private Bitmap mCustomTileset;
	private ImageButton mBrowse;
	private Bitmap mCustomTile;

	public TilesetPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		mEntries = context.getResources().getStringArray(R.array.tileNames);
		mEntryValues = context.getResources().getStringArray(R.array.tileValues);
	}

	@Override
	protected View onCreateView(ViewGroup parent)
	{
		mRoot = (LinearLayout)super.onCreateView(parent);

		createChoices();

		mTilesetUI = (ViewGroup)mRoot.findViewById(R.id.customTilesUI);
		mTileW = (EditText)mRoot.findViewById(R.id.tileW);
		mTileH = (EditText)mRoot.findViewById(R.id.tileH);
		((RadioButton)mRoot.findViewById(R.id.custom_tiles)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				setCustomUIEnabled(isChecked);
			}
		});
		mBrowse = (ImageButton)mRoot.findViewById(R.id.browse);
		mBrowse.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				choseCustomTilesetImage();
			}
		});
		mTilesetPath = (TextView)mRoot.findViewById(R.id.image_path);

		return mRoot;
	}

	@Override
	protected void onBindView(View view)
	{
		super.onBindView(view);

		SharedPreferences prefs = getSharedPreferences();
		String currentValue = prefs.getString("tileset", "TTY");

		for(int i = 0; i < mEntryValues.length; i++)
		{
			if(currentValue.equals(mEntryValues[i]))
			{
				((RadioButton)mRoot.getChildAt(i)).setChecked(true);
				break;
			}
		}

		mTilesetPath.setText(prefs.getString("customTileset", ""));
		mTileW.setText(Integer.toString(prefs.getInt("customTileW", 32)));
		mTileH.setText(Integer.toString(prefs.getInt("customTileH", 32)));
		updateTileIcon();

		mTileW.addTextChangedListener(updateCustom);
		mTileH.addTextChangedListener(updateCustom);
		mTilesetPath.addTextChangedListener(updateCustom);
	}

	private void choseCustomTilesetImage()
	{
		Intent intent = new Intent();
		intent.setType("image/*");
		intent.setAction(Intent.ACTION_GET_CONTENT);
		mSettings.startActivityForResult(Intent.createChooser(intent, "Select Tileset Image"), GET_IMAGE_REQUEST);
	}

	@Override
	public boolean onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if(resultCode == Activity.RESULT_OK && requestCode == GET_IMAGE_REQUEST)
		{
			String path = queryPath(data.getData());
			mTilesetPath.setText(path);
		}
		return requestCode == GET_IMAGE_REQUEST;
	}

	public String queryPath(Uri uri)
	{
		String[] projection = {MediaStore.Images.Media.DATA};
		Cursor cursor = mSettings.managedQuery(uri, projection, null, null, null);
		if(cursor != null)
		{
			int column_index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
			if(column_index >= 0)
			{
				cursor.moveToFirst();
				String path = cursor.getString(column_index);
				if(path != null && path.length() > 0)
					return path;
			}
		}

		return uri.getPath();
	}

	private void setCustomUIEnabled(boolean enabled)
	{
		if(enabled)
			persistCustom();
		setTreeEnabled(mTilesetUI, enabled);
		mTilesetPath.setEnabled(false);
	}

	private void setTreeEnabled(View view, boolean enabled)
	{
		view.setEnabled(enabled);
		if(view instanceof ViewGroup)
		{
			ViewGroup group = (ViewGroup)view;
			for(int i = 0; i < group.getChildCount(); i++)
				setTreeEnabled(group.getChildAt(i), enabled);
		}
	}

	private void createChoices()
	{
		for(int i = mEntries.length - 1; i >= 0; i--)
		{
			RadioButton button = new RadioButton(getContext());
			button.setText(mEntries[i]);
			button.setTag(mEntryValues[i]);
			button.setOnCheckedChangeListener(tilesetChecked);
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			mRoot.addView(button, 0, params);
		}
	}

	private void persistTileset(String id, int tileW, int tileH, boolean custom)
	{
		SharedPreferences.Editor editor = getEditor();
		if(editor != null)
		{
			editor.putString("tileset", id);
			editor.putInt("tileW", tileW);
			editor.putInt("tileH", tileH);
			editor.putBoolean("customTiles", custom);
			if(custom)
			{
				editor.putString("customTileset", id);
				editor.putInt("customTileW", tileW);
				editor.putInt("customTileH", tileH);
			}
			if(shouldCommit())
			{
				try
				{
					editor.apply();
				}
				catch(AbstractMethodError unused)
				{
					editor.commit();
				}
			}
		}
	}

	private CompoundButton.OnCheckedChangeListener tilesetChecked = new CompoundButton.OnCheckedChangeListener()
	{
		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
		{
			if(isChecked)
			{
				String id = (String)buttonView.getTag();
				int tileW, tileH;
				if(id.endsWith("32"))
				{
					tileW = 32;
					tileH = 32;
				} else
				{
					tileW = 12;
					tileH = 20;

				}
				persistTileset(id, tileW, tileH, false);
			}
		}
	};

	private TextWatcher updateCustom = new TextWatcher()
	{
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after)
		{

		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count)
		{

		}

		@Override
		public void afterTextChanged(Editable s)
		{
			persistCustom();
		}
	};

	private void persistCustom()
	{
		try
		{
			String id = mTilesetPath.getText().toString();
			int tileW = Integer.parseInt(mTileW.getText().toString());
			int tileH = Integer.parseInt(mTileH.getText().toString());
			persistTileset(id, tileW, tileH, true);
			updateTileIcon();
		}
		catch(NumberFormatException e)
		{

		}
	}

	private void updateTileIcon()
	{
		String newPath = mTilesetPath.getText().toString();
		if(!newPath.equals(mCustomTilesetPath))
		{
			mCustomTilesetPath = newPath;
			if(mCustomTileset != null)
				mCustomTileset.recycle();
			if(newPath.length() > 0)
			{
				try
				{
					mCustomTileset = BitmapFactory.decodeFile(newPath);
					if(mCustomTileset == null)
						Toast.makeText(getContext(), "Error loading: " + newPath, Toast.LENGTH_SHORT).show();

				}
				catch(Exception e)
				{
					Toast.makeText(getContext(), "Error loading " + newPath + ": " + e.toString(), Toast.LENGTH_LONG).show();
					mCustomTileset = null;
				}
				catch(OutOfMemoryError e)
				{
					Toast.makeText(getContext(), "Error loading " + newPath + ": Out of memory", Toast.LENGTH_LONG).show();
					mCustomTileset = null;
				}
			}
			else
			{
				mCustomTileset = null;
			}
		}

		Bitmap tile = getTile();
		if(tile == null)
		{
			Drawable drawable = getContext().getResources().getDrawable(android.R.drawable.ic_menu_gallery);
			drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
			mBrowse.setImageDrawable(drawable);
		}
		else
		{
			BitmapDrawable drawable = new BitmapDrawable(getContext().getResources(), tile);
			drawable.setBounds(0, 0, tile.getWidth(), tile.getHeight());
			mBrowse.setImageDrawable(drawable);
			if(mCustomTile != null)
				mCustomTile.recycle();
			mCustomTile = tile;
		}
	}

	private Bitmap getTile()
	{
		if(mCustomTileset == null)
			return null;

		try
		{
			int tileW = Integer.parseInt(mTileW.getText().toString());
			int tileH = Integer.parseInt(mTileH.getText().toString());

			return Bitmap.createBitmap(mCustomTileset, 0, 0, tileW, tileH);
		}
		catch(IllegalArgumentException e)
		{
			Toast.makeText(getContext(), "Invalid tile or tileset dimensions", Toast.LENGTH_SHORT);
		}
		return null;
	}

	public void setActivity(Settings settings)
	{
		mSettings = settings;
	}
}
