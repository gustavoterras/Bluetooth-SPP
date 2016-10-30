package br.com.app.bluetoohandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static br.com.app.bluetoohandroid.BluetoothService.readFromBluetooh;

public class MainActivity extends Activity implements IBluetoothServiceEventReceiver
{

	/**
	 * Der {@link PowerManager.WakeLock}, der das Handy wach hält
	 */
	@NotNull
	private PowerManager.WakeLock wakeLock;
	private EditText messagem;
	private ListView listView;
	private List<String> messagens = new ArrayList<>();

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		messagem = (EditText) findViewById(R.id.messagem);
		listView = (ListView) findViewById(R.id.listview);

	    // Wake lock beziehen
	    final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "do_not_turn_off");

	    // Bluetooth initialisieren
	    BluetoothService.initialize(getApplicationContext(), this);
    }

	public void send(View view){

		if(TextUtils.isEmpty(messagem.getText().toString()))
			messagem.setError("Mensagem inválida!");
		else
			if(!TextUtils.isEmpty(messagem.getError()))
				messagem.setError(null);
			else {
				BluetoothService.sendToTarget(messagem.getText().toString());
				messagens.add(messagem.getText().toString());
				messagem.setText(null);

				messagens.add(BluetoothService.readFromBluetooh());
				populateList(messagens);
			}
	}

	private void populateList(List<String> messagens){
		listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messagens));
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!BluetoothService.requestEnableBluetooth(this)) {
			bluetoothEnabled();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		wakeLock.acquire();
		BluetoothService.registerBroadcastReceiver(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		wakeLock.release();
		BluetoothService.unregisterBroadcastReceiver(this);
		BluetoothService.disconnect();
	}

	/**
	 * Bluetooth wird aktiviert
	 */
	@Override
	public void bluetoothEnabling() {
		// Text setzen
		((TextView) findViewById(R.id.textViewState)).setText(R.string.value_enabling);
	}

	/**
	 * Bluetooth wurde aktiviert
	 */
	@Override
	public void bluetoothEnabled() {
		Toast.makeText(this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show();

		// Text setzen
		((TextView) findViewById(R.id.textViewState)).setText(R.string.value_enabled);

		// Gerät suchen
		startSearchDeviceIntent();
	}

	/**
	 * Sucht nach einem Bluetooth-Gerät zum Verbinden
	 */
	private void startSearchDeviceIntent() {
		Intent serverIntent = new Intent(this, DeviceListActivity.class);
		startActivityForResult(serverIntent, IntentRequestCodes.BT_SELECT_DEVICE);
	}

	/**
	 * Bluetooth wird deaktiviert
	 */
	@Override
	public void bluetoothDisabling() {
		// Text setzen
		((TextView) findViewById(R.id.textViewState)).setText(R.string.value_disabling);
	}

	/**
	 * Bluetooth wurde deaktiviert
	 */
	@Override
	public void bluetoothDisabled() {
		Toast.makeText(this, R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();

		// Text setzen
		((TextView) findViewById(R.id.textViewState)).setText(R.string.value_disabled);
		((TextView) findViewById(R.id.textViewTarget)).setText(R.string.value_na);
	}

	/**
	 * Bluetooth verbunden mit einem Gerät
	 *
	 * @param name    Der Name des Gerätes
	 * @param address Die MAC-Adresse des Gerätes
	 */
	@Override
	public void connectedTo(@NotNull String name, @NotNull String address) {
		((TextView)findViewById(R.id.textViewTarget)).setText(name + " (" + address + ")");
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case IntentRequestCodes.BT_REQUEST_ENABLE: {
				if (BluetoothService.bluetoothEnabled()) {
					bluetoothEnabled();
				}
				break;
			}

			case IntentRequestCodes.BT_SELECT_DEVICE: {
				if (resultCode == Activity.RESULT_OK) {
					// Get the device MAC address
					String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

					// Und verbinden
					BluetoothService.connectToDevice(address);
				}
			}

			default: {
				super.onActivityResult(requestCode, resultCode, data);
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (BluetoothService.bluetoothAvailable()) {
			MenuInflater inflater = getMenuInflater();
			inflater.inflate(R.menu.option_menu, menu);
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.scan:
				
				if (!BluetoothService.bluetoothEnabled()) {
					BluetoothService.requestEnableBluetooth(this);
					return true;
				}
				
				// Gerät suchen
				startSearchDeviceIntent();
				return true;
		}
		return false;
	}
}
