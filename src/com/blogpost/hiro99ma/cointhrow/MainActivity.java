package com.blogpost.hiro99ma.cointhrow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.blogpost.hiro99ma.nfc.NfcFactory;

public class MainActivity extends Activity {

    private final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		boolean ret = NfcFactory.nfcCreate(this);
		if (!ret) {
			Toast.makeText(MainActivity.this, "fail create...", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		Log.d(TAG, "onResume()");
		super.onResume();

		Log.d(TAG, "nfcResume");
		boolean ret = NfcFactory.nfcResume(MainActivity.this);
		if(!ret) {
			Log.e(TAG, "fail : resume");
			Toast.makeText(MainActivity.this, "No NFC...", Toast.LENGTH_LONG).show();
			finish();
		}
	}

	@Override
	public void onPause() {
		Log.d(TAG, "onPause()");
		NfcFactory.nfcPause(this);
		super.onPause();
	}
	
	@Override
	public void onNewIntent(Intent intent) {
		Log.d(TAG, "onNewIntent");
		super.onNewIntent(intent);
		
		boolean ret = NfcFactory.nfcAction(intent);
		if (!ret) {
			Toast.makeText(this, "fail...", Toast.LENGTH_LONG).show();
		}
	}
}
