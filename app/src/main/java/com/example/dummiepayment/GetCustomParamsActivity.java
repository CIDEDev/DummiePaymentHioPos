package com.example.dummiepayment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class GetCustomParamsActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Intent resultIntent = new Intent(getIntent().getAction());
		try
		{
			// Obtenim els bytes del logo
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int readLen = 0;
			byte[] readBuffer = new byte[1024];
			InputStream is = getAssets().open("logo.png");
			while ((readLen = is.read(readBuffer)) > 0)
			{
				baos.write(readBuffer, 0, readLen);
			}
			
			resultIntent.putExtra("Logo", baos.toByteArray());
			resultIntent.putExtra("Name", "Webinar Payment");
			
			setResult(RESULT_OK, resultIntent);
			finish();
		}
		catch (Exception e)
		{
			resultIntent.putExtra("ErrorMessage", e.getClass() + " " + e.getMessage());
			
			setResult(RESULT_CANCELED, resultIntent);
			finish();
		}
		
		
		
	}
}
