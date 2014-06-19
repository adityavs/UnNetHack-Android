package com.tbd.UnNetHack;

import java.util.Set;

import android.app.Activity;
import android.util.FloatMath;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.tbd.UnNetHack.Input.Modifier;

public class AmountSelector
{
	public interface Listener
	{
		void onDismissCount(MenuItem item, int amount);
	}

	private View mRoot;
	private Tileset mTileset;
	private TextView mAmountText;
	private int mMax;
	private MenuItem mItem;
	private Activity mContext;
	private Listener mListener;

	private AmountTuner mAmountTuner = new AmountTuner();
	
	class AmountTuner implements Runnable
	{
		private SeekBar mSeek;
		private View mView;
		private boolean mActive;
		private boolean mIncrease;
		long mTime;

		public void start(View v, SeekBar seek, boolean increase)
		{
			mView = v;
			mSeek = seek;
			mIncrease = increase;
			seek.incrementProgressBy(increase ? 1 : -1);
			mActive = true;
			mTime = System.currentTimeMillis() + 250;
			v.postDelayed(this, 250);
		}

		public void stop(View v)
		{
			if(v == this.mView)
				mActive = false;
		}

		public void run()
		{
			if(!mActive)
				return;

			long dt = (int)(System.currentTimeMillis() - mTime);
			int max = mSeek.getMax() / 10;
			
			int amount = 1;
			if(dt > 700 && max > 0)
				amount = Math.min(max, (int)Math.pow(3.0, (double)dt / 700.0));

			mSeek.incrementProgressBy(mIncrease ? amount : -amount);
			mView.postDelayed(this, 100);
		}
	};

	// ____________________________________________________________________________________
	public AmountSelector(Listener listener, Activity context, NetHackIO io, Tileset tileset, MenuItem item)
	{
		mItem = item;
		mListener = listener;
		mContext = context;
		mTileset = tileset;
		mMax = item.getMaxCount();

		mRoot = Util.inflate(mContext, R.layout.amount_selector, R.id.dlg_frame);
		ImageView tileView = (ImageView)mRoot.findViewById(R.id.amount_tile);
		final SeekBar seek = ((SeekBar)mRoot.findViewById(R.id.amount_slider));
		if(item.getTile() != 0 && mTileset.hasTiles())
		{
			tileView.setVisibility(View.VISIBLE);
			tileView.setImageDrawable(new TileDrawable(mTileset, item.getTile()));
		}
		else
		{
			tileView.setVisibility(View.GONE);
		}

		((TextView)mRoot.findViewById(R.id.amount_title)).setText(" " + item.getName().toString());

		mAmountText = (TextView)mRoot.findViewById(R.id.amount);
		int pad = 9;
		while(pad <= mMax)
			pad = pad * 10 + 9;
		int w = (int)FloatMath.floor(mAmountText.getPaint().measureText(" " + Integer.toString(pad)));
		mAmountText.setWidth(w);

		seek.setOnSeekBarChangeListener(new OnSeekBarChangeListener()
		{
			public void onStopTrackingTouch(SeekBar seekBar)
			{
			}

			public void onStartTrackingTouch(SeekBar seekBar)
			{
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				mAmountText.setText(Integer.toString(progress));
			}
		});
		seek.setMax(mMax);
		seek.setProgress(mMax);

		mRoot.findViewById(R.id.btn_inc).setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					mAmountTuner.start(v, seek, true);
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					mAmountTuner.stop(v);
				}
				return true;
			}
		});

		mRoot.findViewById(R.id.btn_dec).setOnTouchListener(new OnTouchListener()
		{
			@Override
			public boolean onTouch(View v, MotionEvent event)
			{
				if(event.getAction() == MotionEvent.ACTION_DOWN)
				{
					mAmountTuner.start(v, seek, false);
				}
				else if(event.getAction() == MotionEvent.ACTION_UP)
				{
					mAmountTuner.stop(v);
				}
				return true;
			}
		});

		mRoot.findViewById(R.id.btn_0).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				if(mRoot != null)
				{
					dismiss(seek.getProgress());
				}
			}
		});
		mRoot.findViewById(R.id.btn_1).setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dismiss(-1);
			}
		});

		seek.requestFocus();
		seek.requestFocusFromTouch();
	}

	// ____________________________________________________________________________________
	public KeyEventResult handleKeyDown(char ch, int nhKey, int keyCode, Set<Modifier> modifiers, int repeatCount, boolean bSoftInput)
	{
		if(mRoot == null)
			return KeyEventResult.IGNORED;

		switch(keyCode)
		{
		case KeyEvent.KEYCODE_BACK:
			dismiss(-1);
		break;

		default:
			return KeyEventResult.RETURN_TO_SYSTEM;
		}
		return KeyEventResult.HANDLED;
	}

	public void dismiss(int amount)
	{
		if(mRoot != null)
		{
			mRoot.setVisibility(View.GONE);
			((ViewGroup)mRoot.getParent()).removeView(mRoot);
			mRoot = null;
			mListener.onDismissCount(mItem, amount);
		}
	}
}
