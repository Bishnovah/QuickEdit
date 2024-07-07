
package com.hava.quickedit;

import android.app.*;
import android.os.*;
import android.widget.*;
import java.util.*;
import android.view.*;
import android.graphics.*;
import android.speech.tts.*;
import android.media.*;
import java.io.*;
import android.speech.*;
import android.content.*;
import android.content.pm.*;
import android.*;
import android.view.animation.*;
import android.graphics.drawable.*;
import android.widget.NumberPicker.*;

public class MainActivity extends Activity{
	
	protected TextView get_feedback_title = null;
	protected TextView get_feedback_content = null;
	
	private String current_page = "home_page";
	private String prev_page = "";
	
	private Handler handler;
	private Animation fade;
	
	private LinearLayout pages = null;
	private EditText note = null;
	
	private Vector page_number = new Vector();
	private TextView tag_page = null;
	
	private String voiceInput = "";
	private List<ResolveInfo> activities = null;
	private TextToSpeech voice;
	private MediaRecorder audioRecorder;
	private SpeechRecognizer speech = null;
	private Intent intent = null;
	private PackageManager packageManager = null;
	
	//private File listingDB = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/maya_listingDB.awb");
	private File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/quick_edit.txt");
	
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
		
		initializer();
		
		new_page();
		//new_page();
    }

	@Override
	public void onBackPressed(){

		if(current_page.equals("home_page")){
			//System.exit(0);
			super.onBackPressed();
		}
		
		else{
			current_page = "home_page";
		}

	}
	
	private void feedback(String title, String content){

		get_feedback_title.setText(title);
		get_feedback_content.setText(content);
		findViewById(R.id.feedback_page).setVisibility(1);

		LinearLayout xbutton = (LinearLayout) findViewById(R.id.button_feedback_ok);
		xbutton.setEnabled(true);

	}

	public void close_feedback(View view){

		findViewById(R.id.feedback_page).setVisibility(8);
		get_feedback_title.setText("");
		get_feedback_content.setText("");

		LinearLayout xbutton = (LinearLayout) findViewById(R.id.button_feedback_ok);
		xbutton.setEnabled(false);
		
	}
	
	private void linear_layout_size(LinearLayout layout, int width, int height){
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		layout.setLayoutParams(params);
	}

	private void text_view_style(TextView textview, int typeface, String color){

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT , LinearLayout.LayoutParams.WRAP_CONTENT);
		Typeface font = getResources().getFont(R.drawable.arial);

		textview.setTextColor(Color.parseColor(color));
		textview.setTypeface(font, typeface);
		textview.setLayoutParams(params);
	}
	
	private void new_page(){
		
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(550, 920);
		Typeface font = getResources().getFont(R.drawable.arial);
		
		note = new EditText(this);
		note.setBackgroundResource(R.drawable.rec4);
		note.setLayoutParams(params);
		note.setGravity(Gravity.TOP);
		note.setTextColor(Color.parseColor("#d536085B"));
		note.setTypeface(font, Typeface.NORMAL);
		note.setTextSize(14);
		//note.setFocusable(false);
		
		LinearLayout primaryContainer = new LinearLayout(this);
		linear_layout_size(primaryContainer, 580, 1050);
		primaryContainer.setBackgroundResource(R.drawable.shadow1);
		primaryContainer.addView(note);
		
		page_number.add(page_number.size() + 1);
		
		//tag_page = (TextView) findViewById(R.id.current_page);
		//tag_page.setText("1");
		tag_page = (TextView) findViewById(R.id.total_page);
		tag_page.setText("" + page_number.size());
		
		pages.addView(primaryContainer);
		
	}
	
	
	
	private void requestStoragePermission(File file){

		String requiredPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;

		// If the user previously denied this permission then show a message explaining why
		// this permission is needed

		if (this.checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_GRANTED){} 
		
		if (this.checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED){
		
			Toast.makeText(getApplicationContext(), "Saved successfully.", Toast.LENGTH_LONG).show(); 
			
			try{
				requestPermissions(new String[]{requiredPermission}, 101);
				
			}catch(Exception ee){
				feedback("Error", "Storage Read_Write error");
			}
		}

		try{
			if(!(file.exists())){

				file.createNewFile();
				//write_to_storage(doc);	
			}

		}catch(Exception e){
			feedback("Error", "Storage Read_Write error " + e.toString());
		}		
	}

	private void request_permission(String intented_permission) {

        String requiredPermission = intented_permission;

        // If the user previously denied this permission then show a message explaining why
        // this permission is needed

        if (this.checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
			requestPermissions(new String[]{requiredPermission}, 101);
			//request_permission(intented_permission);
        }
    }
	
	private void initializer(){	

		get_feedback_title = (TextView) findViewById(R.id.feedback_title);
		get_feedback_content = (TextView) findViewById(R.id.feedback_content);

		pages = (LinearLayout) findViewById(R.id.pages);
		
		request_permission(Manifest.permission.RECORD_AUDIO);
		
		requestStoragePermission(file);

		intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"en");
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
		intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
		intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

		runListener();
		
		try{
			voice = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener(){
					@Override
					public void onInit(int status){
						if(status != TextToSpeech.ERROR){
							voice.setLanguage(Locale.ENGLISH);
							voice.setSpeechRate(0.8f);
							voice.setPitch(0.9f);
						}
					}
				});
		}catch(Exception e2){
			feedback("Error", "Speech initialization error");
		}
		
	}
	
	private void runListener(){
		
		packageManager = getPackageManager();
		activities = packageManager.queryIntentActivities(new Intent (RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		
		if (activities.size() == 0) { 
			voiceInput = "error: Recognizer Not Found";
			
		}

		else{
			speech = SpeechRecognizer.createSpeechRecognizer(this);
			speech.setRecognitionListener(new RecognitionListener(){

					@Override
					public void onBeginningOfSpeech(){
						
					}

					@Override
					public void onBufferReceived(byte[] arg0){
						
					}

					@Override
					public void onEndOfSpeech(){
						
					}

					@Override
					public void onError(int arg0){
						stop_animation();
						if(arg0 == 9){
						feedback("Oops!", "Error: Audio permission has not been granted." );
						handler.postDelayed(new Runnable(){
								@Override
								public void run(){
									request_permission(Manifest.permission.RECORD_AUDIO);
								}},2000);
						}
					}

					@Override
					public void onEvent(int arg0, Bundle arg1){
						
					}

					@Override
					public void onPartialResults(Bundle partialResults){
						
					}

					@Override
					public void onReadyForSpeech(Bundle params){
						
					}

					@Override
					public void onResults(Bundle results) {

						ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
						for (String result : matches){
							note.append(result + "\n");
							
							stop_animation();
							
							break;
						}

					}

					@Override
					public void onRmsChanged(float rmsdB){}
				});

		}
		
	}
	
	public void listen(View view){
		
		handler = new Handler();
		
		TextView label = findViewById(R.id.listen_button_label);
		label.setText("");
		
		findViewById(R.id.listen_cir1).setVisibility(1);
		
		delay_animation(200, (LinearLayout) findViewById(R.id.listen_cir2), R.drawable.fade2);
		
		delay_animation(130, (LinearLayout) findViewById(R.id.listen_cir3), R.drawable.fade);
		
		delay_animation(5, (LinearLayout) findViewById(R.id.listen_cir4), R.drawable.fade);
		
		try{
			speech.startListening(intent);
		}catch(Exception ee){feedback("", ee.toString());}
		
		//feedback("", "save");
	}
	
	public void new_page(View view){
		new_page();
		//resize_pages();
	}
	
	public void save(View view){
		save();
	}
	
	public void clear(View view){
		pages.removeAllViews();
		page_number.clear();
		new_page();
	}
	
	private void save(){
		
		StringBuilder text = new StringBuilder();
		
			try{
				
				for(int i = 0; i < pages.getChildCount(); i++){
					LinearLayout page = (LinearLayout) pages.getChildAt(i);
					note = null;
					note = (EditText) page.getChildAt(0);
					text.append(note.getText().toString() + "\n");
				}
				
				ObjectOutputStream writer = new ObjectOutputStream(new DataOutputStream(new FileOutputStream(file)));
				writer.writeObject(text.toString());
				writer.flush();
				writer.close();
				
				Toast.makeText(getApplicationContext(), "Saved successfully.", Toast.LENGTH_LONG).show(); 
				pages.removeAllViews();
				page_number.clear();
				new_page();
				
			}catch(Exception ee){
				feedback("Oops!, Error", "unable to write to device storage.\n" + ee.toString());
				try{
					requestStoragePermission(file);
					feedback("Cool", ee.toString() +"error fixed.");
				}catch(Exception e){
					feedback("Oops! Error", "unable to create new memory storage.\n" + e.toString());
				}
			}
			
	}
	
	private void stop_animation(){
		
		handler.postDelayed(new Runnable()
			{
				@Override
				public void run(){

					LinearLayout layout1 = (LinearLayout) findViewById(R.id.listen_cir2);
					layout1.clearAnimation();
					layout1.animate().cancel();
					
					LinearLayout layout2 = (LinearLayout) findViewById(R.id.listen_cir3);
					layout2.clearAnimation();
					layout2.animate().cancel();
					
					LinearLayout layout3 = (LinearLayout) findViewById(R.id.listen_cir4);
					layout3.clearAnimation();
					layout3.animate().cancel();
					
					findViewById(R.id.listen_cir1).setVisibility(8);
					
					TextView label = findViewById(R.id.listen_button_label);
					label.setText("Listen");
				}
			}, 1000);
			
	}
	
	private void delay_animation(int delay, final LinearLayout layout, final int drawable){
	
		//for(int i = 0; i < 1; i++){
			
			fade = AnimationUtils.loadAnimation(getApplicationContext(), drawable);
			fade.setRepeatCount(Animation.INFINITE);

			handler.postDelayed(new Runnable(){
					@Override
					public void run(){
						layout.startAnimation(fade);
					}
				}, delay);

		//}
	}
}
