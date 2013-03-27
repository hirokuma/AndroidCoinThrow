package com.blogpost.hiro99ma.nfc;

//import android.R;
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.SoundPool;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.util.Log;

import com.blogpost.hiro99ma.cointhrow.R;

/**
 * NFC関係の処理を抜き出した
 *
 * @author hiroshi
 *
 */
public class NfcFactory {

	private final static String TAG = "NfcFactory";

	/*
	 * この辺は必要に応じて変更すること
	 */
    private final static IntentFilter[] mFilters = new IntentFilter[] {
		new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
		new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
	};

    //横に書くとAND条件、縦に書くとOR条件
    private final static String[][] mTechLists = new String[][] {
    	new String[] { Ndef.class.getName() },
    	new String[] { NfcA.class.getName() },
    	new String[] { NfcB.class.getName() },
    	new String[] { NfcF.class.getName() },
    };

    private final static int kSOUND_OK = 0;
    private final static int kSOUND_NG = 1;
    private final static int kSOUND_OH = 2;
    private final static int kMAX_SOUND = 3;
    private static SoundPool mSoundPool;
    private static int[] mSoundNum = new int[kMAX_SOUND];

    public static boolean nfcCreate(Activity activity) {
    	mSoundPool = new SoundPool(kMAX_SOUND, AudioManager.STREAM_MUSIC, 0);
    	mSoundNum[kSOUND_OK] = mSoundPool.load(activity, R.raw.se_maoudamashii_onepoint15, 1);
    	mSoundNum[kSOUND_NG] = mSoundPool.load(activity, R.raw.se_maoudamashii_onepoint03, 1);
    	mSoundNum[kSOUND_OH] = mSoundPool.load(activity, R.raw.se_maoudamashii_onepoint06, 1);
    	return true;
    }
    
    /**
     * onResume()時の動作
     *
     * @param activity		現在のActivity。だいたいthisを渡すことになる。
     * @return				true:NFCタグ検出の準備ができた<br />
     * 						false:できなかった
     */
	public static boolean nfcResume(Activity activity) {
		//NFC
		NfcManager mng = (NfcManager)activity.getSystemService(Context.NFC_SERVICE);
		if (mng == null) {
			Log.e(TAG, "no NfcManager");
			return false;
		}
		NfcAdapter adapter = mng.getDefaultAdapter();
		if (adapter == null) {
			Log.e(TAG, "no NfcService");
			return false;
		}

		//newがnullを返すことはない
		Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(
						activity,
						0,		//request code
						intent,
						0);		//flagなし

		adapter.enableForegroundDispatch(activity, pendingIntent, mFilters, mTechLists);

		return true;
	}

	/**
	 * onPause()時の動作
	 *
	 * @param activity		現在のActivity。だいたいthisを渡すことになる。
	 */
	public static void nfcPause(Activity activity) {
		NfcManager mng = (NfcManager)activity.getSystemService(Context.NFC_SERVICE);
		if (mng == null) {
			Log.e(TAG, "no NfcManager");
			return;
		}
		NfcAdapter adapter = mng.getDefaultAdapter();
		if (adapter == null) {
			Log.e(TAG, "no NfcService");
			return;
		}

		if (activity.isFinishing()) {
			adapter.disableForegroundDispatch(activity);
		}
	}


	/**
	 * IntentからTagを取得する
	 *
	 * @param intent		Tag取得対象のIntent
	 * @return				取得したTag。<br />
	 * 						失敗した場合はnullを返す。
	 */
	private static Tag getTag(Intent intent) {
		//チェック
		String action = intent.getAction();
		if (action == null) {
			Log.e(TAG, "fail : null action");
			return null;
		}
		boolean match = false;
		for (IntentFilter filter : mFilters) {
			if (filter.matchAction(action)) {
				match = true;
				break;
			}
		}
		if (!match) {
			Log.e(TAG, "fail : no match intent-filter");
			return null;
		}

		 return (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
	}


	/**
	 * onNewIntent()で実行したい動作 : 
	 *
	 * @param intent		取得したIntent
	 * @return				true:処理成功<br />
	 * 						false:処理失敗
	 */
	public static boolean nfcAction(Intent intent) {
		//Tag取得
		Tag tag = getTag(intent);
		if (tag == null) {
			return false;
		}

		/***********************************************
		 * 以降に、自分がやりたい処理を書く
		 ***********************************************/

		boolean ret = false;

		Ndef ndef = Ndef.get(tag);
		if (ndef != null) {
			//こいつ、NDEFだ
			boolean chk = false;
			
			//判定
			try {
				ndef.connect();
				NdefMessage nmsg = ndef.getNdefMessage();
				NdefRecord[] nrec = null;
				if (nmsg != null) {
					nrec = nmsg.getRecords();
				}
				if ((nrec != null) && (nrec.length != 0)){
					for (NdefRecord rec : nrec) {
						if ((rec.getTnf() == NdefRecord.TNF_WELL_KNOWN) && Arrays.equals(NdefRecord.RTD_TEXT, rec.getType())) {
							//TNF==WellKnown && Type==T
							byte[] pld = rec.getPayload();
							//b7   : 0...UTF8 1...UTF16
							//b6   : RFU
							//b5-0 : IANA language length
							int langlen = pld[0] & 0x3f;
							if ((pld[langlen + 1] == 'O') && (pld[langlen + 2] == 'K')) {
								chk = true;
							}
						}
					}
				}
			} catch (IOException e) {
				chk = false;
			} catch (FormatException e) {
				chk = false;
			}
			try {
				ndef.close();
			} catch (IOException e) {
				Log.e(TAG, "fail close");
			}
			
			//判定による音鳴動
			if (chk) {
				mSoundPool.play(mSoundNum[kSOUND_OK], 1.0F, 1.0F, 0, 0, 1.0F);
			} else {
				mSoundPool.play(mSoundNum[kSOUND_NG], 1.0F, 1.0F, 0, 0, 1.0F);
			}
			
			ret = true;
		} else {
			mSoundPool.play(mSoundNum[kSOUND_OH], 1.0F, 1.0F, 0, 0, 1.0F);
			Log.e(TAG, "お前など知らぬ");
			ret = false;
		}

		return ret;
	}
}
