package com.example.dummiepayment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;



public class GetVersionActivity extends Activity

{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		onGetVersion();
	}

	/**
	 * Event triggered when a 'GET_VERSION' intent it's filtered. Is the responsibility
	 * of integrator to implement it and set the right result.
	 */
	protected void onGetVersion()
	{
		setVersionResult(14);
	}


	protected void setVersionResult(int version)
	{
		Intent resultIntent = new Intent(getIntent().getAction());

		resultIntent.putExtra("Version", version);

		setResult(RESULT_OK, resultIntent);
		finish();
	}



}
