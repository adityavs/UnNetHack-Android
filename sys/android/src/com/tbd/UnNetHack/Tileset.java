package com.tbd.UnNetHack;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

public class Tileset
{
	private Bitmap mBitmap;
	private Bitmap mOverlay;
	private int mTileW;
	private int mTileH;
	private String mTilesetName = "";
	private int mnCols;
	private Context mContext;
	private boolean mFallbackRenderer;
	private final Map<Integer, Bitmap> mTileCache = new HashMap<Integer, Bitmap>();

	// ____________________________________________________________________________________
	public Tileset(UnNetHack context)
	{
		mContext = context;
	}

	// ____________________________________________________________________________________
	public void setContext(Context context)
	{
		mContext = context;
	}

	// ____________________________________________________________________________________
	public void updateTileset(SharedPreferences prefs, Resources r)
	{
		mFallbackRenderer = prefs.getBoolean("fallbackRenderer", false);

		String tilesetName = prefs.getString("tileset", "TTY");

		boolean TTY = tilesetName.equals("TTY");
		int tileW = prefs.getInt("tileW", 32);
		int tileH = prefs.getInt("tileH", 32);

		if(mTilesetName.equals(tilesetName) && tileW == mTileW && tileH == mTileH)
			return;
		mTilesetName = tilesetName;

		if(!TTY && (tileW <= 0 || tileH <= 0))
		{
			Toast.makeText(mContext, "Invalid tile dimensions (" + mTileW + "x" + mTileH + ")", Toast.LENGTH_LONG).show();
			TTY = true;

		}

		if(!TTY)
		{
			mTileW = tileW;
			mTileH = tileH;

			if(prefs.getBoolean("customTiles", false))
				loadFromFile(tilesetName, prefs);
			else
				loadFromResources(tilesetName, r);

			BitmapDrawable bmpDrawable = (BitmapDrawable)r.getDrawable(R.drawable.overlays);
			mOverlay = bmpDrawable.getBitmap();

			if(mBitmap == null || mOverlay == null)
				TTY = true;
			else
				mnCols = mBitmap.getWidth() / mTileW;

			if(mnCols <= 0)
			{
				Toast.makeText(mContext, "Invalid tileset settings '" + tilesetName + "' (" + mTileW + "x" + mTileH + ")", Toast.LENGTH_LONG).show();
				TTY = true;
			}
		}

		if(TTY)
		{
			clearBitmap();
			mTileW = 0;
			mTileH = 0;
			mnCols = 0;
		}
	}

	// ____________________________________________________________________________________
	private void clearBitmap()
	{
		if(mBitmap != null)
			mBitmap.recycle();
		for(Bitmap bitmap : mTileCache.values())
			bitmap.recycle();
		mTileCache.clear();
		mBitmap = null;
	}

	// ____________________________________________________________________________________
	private void loadFromFile(String tilesetName, SharedPreferences prefs)
	{
		clearBitmap();
		try
		{
			mBitmap = BitmapFactory.decodeFile(tilesetName);
			if(mBitmap == null)
				Toast.makeText(mContext, "Error loading tileset " + tilesetName, Toast.LENGTH_LONG).show();
		}
		catch(Exception e)
		{
			Toast.makeText(mContext, "Error loading " + tilesetName + ": " + e.toString(), Toast.LENGTH_LONG).show();
		}
		catch(OutOfMemoryError e)
		{
			Toast.makeText(mContext, "Error loading " + tilesetName + ": Out of memory", Toast.LENGTH_LONG).show();
		}
	}

	// ____________________________________________________________________________________
	private void loadFromResources(String tilesetName, Resources r)
	{
		int id = r.getIdentifier(tilesetName, "drawable", "com.tbd.UnNetHack");

		clearBitmap();
		if(id > 0)
		{
			BitmapDrawable bmpDrawable = (BitmapDrawable) r.getDrawable(id);
			mBitmap = bmpDrawable.getBitmap();
		}
	}

	// ____________________________________________________________________________________
	private int getTileBitmapOffset(int iTile)
	{
		if(mBitmap == null)
			return 0;
		
		int iRow = iTile / mnCols;
		int iCol = iTile - iRow * mnCols;

		int x = iCol * mTileW;
		int y = iRow * mTileH;
		
		return (x << 16) | y;
	}

	// ____________________________________________________________________________________
	private Bitmap getTile(int iTile)
	{
		if(mBitmap == null)
			return null;
		Bitmap bitmap = mTileCache.get(iTile);
		if(bitmap == null)
		{
			int ofs = getTileBitmapOffset(iTile);

			int x = ofs >> 16;
			int y = ofs & 0xffff;

			try
			{
				bitmap = Bitmap.createBitmap(mBitmap, x, y, mTileW, mTileH);
			}
			catch(Exception e)
			{
				bitmap = Bitmap.createBitmap(mBitmap, 0, 0, 1, 1);
			}
			mTileCache.put(iTile, bitmap);
		}
		return bitmap;
	}

	// ____________________________________________________________________________________
	public int getTileWidth()
	{
		return mTileW;
	}
	
	// ____________________________________________________________________________________
	public int getTileHeight()
	{
		return mTileH;
	}
	
	// ____________________________________________________________________________________
	public Rect getOverlayRect(short overlay)
	{
		return new Rect(0, 0, 32, 32);
	}

	// ____________________________________________________________________________________
	public Bitmap getTileOverlay(short overlay)
	{
		if(overlay == 8)
			return mOverlay;
		return null;
	}

	// ____________________________________________________________________________________
	public boolean hasTiles()
	{
		return mBitmap != null;
	}

	// ____________________________________________________________________________________
	public void drawTile(Canvas canvas, int glyph, Rect dst, TextPaint paint)
	{
		if(mBitmap == null)
			return;
		Rect src = new Rect();
		if(mFallbackRenderer)
		{
			Bitmap bitmap = getTile(glyph);
			src.left = 0;
			src.top = 0;
			src.right = getTileWidth();
			src.bottom = getTileHeight();
			canvas.drawBitmap(bitmap, src, dst, paint);
		}
		else
		{
			//int maxH = canvas.getMaximumBitmapHeight();
			//int maxW = canvas.getMaximumBitmapWidth();

			int ofs = getTileBitmapOffset(glyph);
			src.left = (ofs >> 16) & 0xffff;
			src.top = ofs & 0xffff;
			src.right = src.left + getTileWidth();
			src.bottom = src.top + getTileHeight();
			canvas.drawBitmap(mBitmap, src, dst, paint);
		}
	}
}
