package com.hellw.hostschange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts.Data;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	private static final String TAG = "DownloadActivity";
	private TextView mTv_result;
	private EditText mEt_hosts;

	public StringBuffer sb_mountpath = null;
	private String readResult;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_main);
		super.onCreate(savedInstanceState);
		initView();
	}

	public void onReadHosts(View v) {
		new Thread() {
			public void run() {
				File file = new File("/system/etc/hosts");
				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis);
					BufferedReader br = new BufferedReader(isr);
					String line = null;
					StringBuffer sb = new StringBuffer();
					while ((line = br.readLine()) != null) {
						sb.append(line + "\n");
					}
					if (br != null)
						br.close();
					if (isr != null)
						isr.close();
					if (fis != null)
						fis.close();

					readResult = sb.toString();
					Message msg = Message.obtain();
					msg.obj = "Hosts:\n" + sb.toString() + "\n";
					mHandler2.sendMessage(msg);
					System.out.print(sb.toString());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}.start();
	}

	/**
	 * 获得/system 挂载的设备块(不需要root)
	 * 
	 * @return 挂载的设备块
	 */
	private String getMountPoint() {
		Process process = null;
		try {
			// String[] cmd = new String[] {"sh", "-c", "mount | busybox grep
			// '/system'"};
			// 防止grep命令找不到 使用busybox中的grep
			String[] cmd1 = new String[] { "sh", "-c", "(mount | grep '/system')||(mount | busybox grep '/system')" };
			// String[] cmd = new String[] {"sh", "-c", "mount | grep
			// '/system'"};
			process = Runtime.getRuntime().exec(cmd1);
			MountPathStreamGobbler outGobbler = new MountPathStreamGobbler(process.getInputStream(), "STDOUT_mount");
			outGobbler.start();
			int exitValue = process.waitFor();

			if (exitValue == 0) {
				String arrays[] = sb_mountpath.toString().split(" ");
				return arrays[0];
			} else {
				return null;
			}
		} catch (Exception e) {
			Log.d(TAG, "Unexpected error - Here is what I know: " + e.getMessage());
			return null;
		} finally {
		}
	}

	private Handler mHandler2 = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			String result = (String) msg.obj;
			mTv_result.append("\n" + result);
		}
	};

	/**
	 * 重新挂载为可读
	 */
	private void remount(String insertvalue) {
		String mountPath = getMountPoint();
		String cmd = "mount -o remount,rw " + mountPath + " /system\n";
		// String cmd1 = "mount -o remount,rw
		// /dev/block/platform/msm_sdcc.1/by-name/system1 /system\n";
		// //我设备的挂载点路径，用于测试
		String cmd2 = "mount -o remount,rw /system\n";
		Process process = null;
		try {
			process = Runtime.getRuntime().exec("su");
			DataOutputStream os = new DataOutputStream(process.getOutputStream());
			DataInputStream is = new DataInputStream(process.getInputStream());
			if (null != os) {
				os.writeBytes(cmd2);
				os.flush();
			}
			String format = "echo '%s' >> /system/etc/hosts\n";
			String insert_cmd = String.format(format, insertvalue);
			os.writeBytes(insert_cmd);
			os.flush();
		} catch (Exception e) {
			Log.d(TAG, "Unexpected error: " + e.getMessage());
		} finally {
		}
	}

	/**
	 * 只能是查看是否root，su成功后不会返回结果不能使用suProcess.waitfor
	 * 会造成线程一直阻塞,su只在同一个proccess实例中有效
	 * 
	 * @return
	 */
	public boolean getRoot() {
		boolean retval = false;
		Process suProcess;

		try {
			suProcess = Runtime.getRuntime().exec("su");

			DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
			DataInputStream osRes = new DataInputStream(suProcess.getInputStream());

			if (null != os && null != osRes) {
				os.writeBytes("id\n"); // 检查用户，看是否root成功
				os.flush();

				String currUid = osRes.readLine();
				boolean exitSu = false;
				if (null == currUid) {
					retval = false;
					exitSu = false;
					Log.d(TAG, "ROOT -- null == currUid, error!!!");
				} else if (true == currUid.contains("uid=0")) {
					retval = true;
					exitSu = true;
					Log.d(TAG, "ROOT -- Root Success!!");
				} else {
					retval = false;
					exitSu = true;
					Log.d(TAG, "ROOT -- Root failure!! curr uid = " + currUid);
				}

				if (exitSu) {
					os.writeBytes("exit\n");
					os.flush();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			retval = false;
		}

		return retval;
	}

	class MountPathStreamGobbler extends Thread {
		InputStream is;
		String type;

		MountPathStreamGobbler(InputStream is, String type) {
			this.is = is;
			this.type = type;
		}

		public void run() {
			Log.i(TAG, type + "run()");
			InputStreamReader isr = null;
			BufferedReader br = null;
			PrintWriter pw = null;
			try {
				isr = new InputStreamReader(is);
				br = new BufferedReader(isr);
				String line = null;
				StringBuffer sb = new StringBuffer();
				while ((line = br.readLine()) != null) {
					if (pw != null)
						pw.println(line);
					sb.append(line);
					Log.i(TAG, type + ">" + line);
					System.out.println(type + ">" + line);
				}
				sb_mountpath = sb;
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} finally {
				try {
					if (br != null)
						br.close();
					if (isr != null)
						isr.close();
					if (is != null)
						is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}
	}

	public void onWriteHosts(View v) {
		//检查是否root
		if (!getRoot()) {
			Toast.makeText(MainActivity.this, "You phone not Root!!!", 4).show();
		}
		else {
			new Thread() {
				public void run() {
						String insertValueHost = mEt_hosts.getText().toString();
						System.out.print(insertValueHost);
						if (TextUtils.isEmpty(insertValueHost)) {
							return;
						} else {
							remount(insertValueHost);
						}
				}
			}.start();
		}
	}

	public void initView() {
		mTv_result = (TextView) findViewById(R.id.tv_result);
		mEt_hosts = (EditText) findViewById(R.id.et_hosts);
	}
}
