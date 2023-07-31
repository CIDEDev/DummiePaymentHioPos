package com.example.dummiepayment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.dummiepayment.utils.APIUtils;

import java.util.Properties;

public class InitializeActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Properties properties = APIUtils.parseInitializeParameters(getIntent().getStringExtra("Parameters"));
		onInitialize(properties);
	}

	protected void onInitialize(Properties cloudLicenseProperties)
	{
		// List all received properties
		for (Object objKey : cloudLicenseProperties.keySet())
		{
			String key = (String) objKey;

			System.out.println("CLOUD LICENSE PROPERTY " + key + ": VALUE = " + cloudLicenseProperties.getProperty(key));
		}

		finishInitializeOK();
	}

	protected void finishInitializeOK()
	{
		Intent resultItent = new Intent(getIntent().getAction());
		setResult(RESULT_OK, resultItent);
		finish();
	}

	protected void finishInitializeWithError(String errorMessage)
	{
		Intent resultIntent = new Intent(getIntent().getAction());
		resultIntent.putExtra("ErrorMessage", errorMessage);

		setResult(RESULT_CANCELED, resultIntent);
		finish();
	}


}
