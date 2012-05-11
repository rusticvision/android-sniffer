package edu.droidshark.android.ui.activities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.ActionBar.TabListener;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import edu.droidshark.R;
import edu.droidshark.android.ui.fragments.activity.PacketViewFragment;
import edu.droidshark.android.ui.fragments.activity.SnifferFragment;
import edu.droidshark.constants.SnifferConstants;

public class DroidSharkActivity extends SherlockFragmentActivity
{
	// public static final int EXIT_MENU_ID = 0x8;
	private static final String TAG = "DroidSharkActivity";
	private int currPane = SnifferConstants.SNIFFERPANE;
	private SnifferFragment snifferFragment;
	private PacketViewFragment packetViewFragment;
	private FrameLayout firstPane, secondPane;
	private ActionBar.Tab mSnifTab, mPVTab;
	private ActionBar mActionBar;
	public boolean tcpdumpIsRunning;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		// Check to see if tcpdump is present
		try
		{
			FileInputStream fis = openFileInput("tcpdump");
			fis.close();
		} catch (FileNotFoundException e)
		{
			try
			{
				// Create tcpdump
				InputStream is = getResources().openRawResource(R.raw.tcpdump);
				FileOutputStream fos = openFileOutput("tcpdump", MODE_PRIVATE);
				byte[] b = new byte[1801155];
				is.read(b);
				fos.write(b);

				is.close();
				fos.close();

				Runtime.getRuntime().exec(
						new String[] { "chmod", "755",
								getFilesDir().getAbsolutePath() + "/tcpdump" });

			} catch (Exception ex)
			{
				Log.e(getClass().getSimpleName(),
						"Exception creating tcpdump, message="
								+ ex.getMessage());
			}
		} catch (IOException e)
		{
			Log.e(getClass().getSimpleName(),
					"IOException, message=" + e.getMessage());
		}

		setContentView(R.layout.main);

		firstPane = (FrameLayout) findViewById(R.id.first_pane);
		secondPane = (FrameLayout) findViewById(R.id.second_pane);

		if (SnifferConstants.DEBUG)
			Log.d(TAG, "Running on device: " + android.os.Build.MODEL);

		// Fragments are saved in the instance state by default, so they don't
		// have to be recreated.
		if (savedInstanceState == null)
		{
			snifferFragment = new SnifferFragment();
			packetViewFragment = new PacketViewFragment();
			FragmentTransaction transaction = getSupportFragmentManager()
					.beginTransaction();

			transaction.add(R.id.first_pane, snifferFragment, "sniffer");
			transaction.add(R.id.second_pane, packetViewFragment, "packetView");
			transaction.commit();
		} else
		{
			snifferFragment = (SnifferFragment) getSupportFragmentManager()
					.findFragmentByTag("sniffer");
			packetViewFragment = (PacketViewFragment) getSupportFragmentManager()
					.findFragmentByTag("packetView");
			currPane = savedInstanceState.getInt("currPane");
		}

		// BEGIN FRAGMENT STUFF
		if (currPane == SnifferConstants.SNIFFERPANE)
			showSniffer();
		else if (currPane == SnifferConstants.PACKETVIEWPANE)
			showPacketView();

		mActionBar = getSupportActionBar();
		mActionBar.setDisplayHomeAsUpEnabled(false);
		mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mSnifTab = mActionBar
				.newTab()
				.setText(R.string.sniffer)
				.setTabListener(
						new ActionTabListener<SnifferFragment>(this, "Snif",
								SnifferFragment.class));
		if (currPane == SnifferConstants.SNIFFERPANE)
			mActionBar.addTab(mSnifTab, true);
		else
			mActionBar.addTab(mSnifTab, false);

		mPVTab = mActionBar
				.newTab()
				.setText(R.string.packet_view)
				.setTabListener(
						new ActionTabListener<PacketViewFragment>(this, "PV",
								PacketViewFragment.class));
		if (currPane == SnifferConstants.PACKETVIEWPANE)
			mActionBar.addTab(mPVTab, true);
		else
			mActionBar.addTab(mPVTab, false);

		// setup shared preferences
		// prefs = PreferenceManager.getDefaultSharedPreferences(this);
		// prefs.registerOnSharedPreferenceChangeListener(this);
	}

	// needed for ActionBar
	@Override
	protected void onPostCreate(Bundle savedInstanceState)
	{
		super.onPostCreate(savedInstanceState);
		// mActionBarHelper.onPostCreate(savedInstanceState);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		tcpdumpIsRunning = isTCPDumpRunning();

		if (SnifferConstants.DEBUG)
			Log.d(TAG, "tcpdumpRunning=" + tcpdumpIsRunning);

		// Disable/enable start stop button as appropriate
		if (tcpdumpIsRunning)
		{
			snifferFragment.getView().findViewById(R.id.startButton)
					.setEnabled(false);
			snifferFragment.getView().findViewById(R.id.stopButton)
					.setEnabled(true);
		} else
		{
			snifferFragment.getView().findViewById(R.id.startButton)
					.setEnabled(true);
			snifferFragment.getView().findViewById(R.id.stopButton)
					.setEnabled(false);
		}
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

	}

	/**
	 * Check to see whether tcpdump is already running
	 * 
	 * @return true if tcpdump is running
	 */
	private boolean isTCPDumpRunning()
	{
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE))
		{
			if ("edu.droidshark.android.services.TCPDumpService"
					.equals(service.service.getClassName()))
			{
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = this.getSupportMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		// Calling super after populating the menu is necessary here to ensure
		// that the action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);

		outState.putInt("currPane", currPane);
	}

	@Override
	protected void onNewIntent(Intent intent)
	{
		setIntent(intent);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection
		switch (item.getItemId())
		{
		case R.id.exit:
			finish();
			return true;

			// case R.id.settings:
			// Intent prefsActivity = new Intent(this, MainPreferences.class);
			// startActivity(prefsActivity);
			// return true;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * Checks the wifi status
	 */
	public void checkWifi()
	{
		WifiManager wifiMgr = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (!wifiMgr.isWifiEnabled() && !("sdk".equals(Build.MODEL)))
		{
			// DialogFragment df = new WifiDisabledAlertFragment();
			// if (getSupportFragmentManager().findFragmentByTag("wifialert") ==
			// null)
			// df.show(getSupportFragmentManager(), "wifialert");
		}
	}

	/**
	 * Closes the soft keyboard
	 * 
	 * @param windowToken
	 *            The field's(ie EditText) window token (use getWindowToken())
	 *            that currently is using the soft keyboard.
	 */
	public void closeIME(IBinder windowToken)
	{
		InputMethodManager mgr = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
		mgr.hideSoftInputFromWindow(windowToken, 0);
	}

	public void showSniffer()
	{
		firstPane.setVisibility(View.VISIBLE);
		secondPane.setVisibility(View.GONE);
		currPane = SnifferConstants.SNIFFERPANE;

		if (mSnifTab != null)
			mActionBar.setSelectedNavigationItem(mSnifTab.getPosition());
	}

	public void showPacketView()
	{
		firstPane.setVisibility(View.GONE);
		secondPane.setVisibility(View.VISIBLE);
		currPane = SnifferConstants.PACKETVIEWPANE;

		if (mPVTab != null)
			mActionBar.setSelectedNavigationItem(mPVTab.getPosition());
	}

	public class ActionTabListener<T extends SherlockFragment> implements
			TabListener
	{
		private final Activity mActivity;
		private final String mTag;

		// private final Class<T> mClass;

		/**
		 * @param activity
		 * @param tag
		 * @param clz
		 */
		public ActionTabListener(Activity activity, String tag, Class<T> clz)
		{
			mActivity = activity;
			mTag = tag;
			// mClass = clz;
		}

		public void onTabReselected(Tab tab, FragmentTransaction unused)
		{
			// User selected the already selected tab. do nothing

		}

		public void onTabSelected(Tab tab, FragmentTransaction unused)
		{
			// Check if fragment is already initialized
			if (mTag == "Snif")
			{
				((DroidSharkActivity) mActivity).showSniffer();
			} else if (mTag == "PV")
			{
				((DroidSharkActivity) mActivity).showPacketView();
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction unused)
		{
			// do nothing
		}
	}
}