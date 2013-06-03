package com.littleinc.MessageMe.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.coredroid.ui.CoreActivity;
import com.coredroid.util.LogIt;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.littleinc.MessageMe.MessageMeApplication;
import com.littleinc.MessageMe.MessageMeConstants;
import com.littleinc.MessageMe.R;
import com.littleinc.MessageMe.bo.CountryCode;
import com.littleinc.MessageMe.util.StringUtil;

public class CountryListActivity extends CoreActivity {	

	private ListView countryList;

	private EditText searchBox;

	private PhoneAdapter adapter;

	/**
	 * Intent extras to specify the text shown in the UI.
	 */
	public static final String EXTRA_INPUT_TITLE = "cla_title";
	public static final String EXTRA_INPUT_LEGEND_TEXT = "cla_legend_text";
	
	private static final long TEXT_CHANGE_THRESHOLD = 500; // milliseconds

	private static final int UPDATE_SEARCH_MESSAGE = 100;

	private List<CountryCode> countries;
	
	/**
     * Ordered list of the popular country identifiers to show at the 
     * top of the country chooser list.
     */
    private static final String[] sCountryCodesPopular = { "US", "GB", "BR",
            "FR", "DE", "JP", "RU", "SA", "KR", "TR" };

	/**
	 * Ordered list of the country identifiers for use in the main
	 * country chooser list.
	 */
    private static final String[] sCountryCodesAll = { "AF", "AL", "DZ", "AS",
            "AD", "AO", "AI", "AG", "AR", "AM", "AW", "AU", "AT", "AZ", "BS",
            "BH", "BD", "BB", "BY", "BE", "BZ", "BJ", "BM", "BT", "BO", "BA",
            "BW", "BR", "IO", "BN", "BG", "BF", "BI", "KH", "CM", "CA", "CV",
            "KY", "CF", "TD", "CL", "CN", "CX", "CC", "CO", "CG", "CK", "CR",
            "CI", "HR", "CU", "CW", "CY", "CZ", "CD", "DK", "DJ", "DM", "DO",
            "EC", "EG", "SV", "GQ", "ER", "EE", "ET", "FK", "FO", "FJ", "FI",
            "FR", "GF", "PF", "GA", "GM", "GE", "DE", "GH", "GI", "GR", "GL",
            "GD", "GP", "GU", "GT", "GG", "GN", "GW", "GY", "HT", "VA", "HN",
            "HK", "HU", "IS", "IN", "ID", "IR", "IQ", "IE", "IM", "IL", "IT",
            "JM", "JP", "JE", "JO", "KZ", "KE", "KI", "KW", "KG", "LA", "LV",
            "LB", "LS", "LR", "LY", "LI", "LT", "LU", "MO", "MK", "MG", "MW",
            "MY", "MV", "ML", "MT", "MH", "MQ", "MR", "MU", "YT", "MX", "FM",
            "MD", "MC", "MN", "ME", "MS", "MA", "MZ", "MM", "NA", "NR", "NP",
            "NL", "NC", "NZ", "NI", "NE", "NG", "NU", "NF", "MP", "NO", "OM",
            "PK", "PW", "PS", "PA", "PG", "PY", "PE", "PH", "PL", "PT", "PR",
            "QA", "RE", "RO", "RU", "RW", "BL", "SH", "KN", "LC", "MF", "PM",
            "VC", "WS", "SM", "ST", "SA", "SN", "RS", "SC", "SL", "SG", "SX",
            "SK", "SI", "SB", "SO", "ZA", "SS", "KR", "ES", "LK", "SD", "SR",
            "SJ", "SZ", "SE", "CH", "SY", "TW", "TJ", "TZ", "TH", "TL", "TG",
            "TK", "TO", "TT", "TN", "TR", "TM", "TC", "TV", "UG", "UA", "AE",
            "GB", "US", "UY", "UZ", "VU", "VE", "VN", "VG", "VI", "WF", "YE",
            "ZM", "ZW" };
	
	private static final List<CountryCode> sCountryListAll = new ArrayList<CountryCode>();
	
	private static final List<CountryCode> sCountryListPopular = new ArrayList<CountryCode>();
	
	// Build up the localized lists of country names
    static {
        Context context = MessageMeApplication.getInstance();
        Resources resources = context.getResources();
        String packageName = context.getPackageName();
        
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        
        for (String countryInitials : sCountryCodesPopular) {
            int localizedCountryNameResId = resources.getIdentifier(countryInitials, "string", packageName);
            
            CountryCode country = new CountryCode(
                    context.getString(localizedCountryNameResId),
                    countryInitials,
                    phoneUtil.getCountryCodeForRegion(countryInitials));
            
            // LogIt.d(CountryCode.class, "Add country to list", country);
            sCountryListPopular.add(country);
        }
        
        for (String countryInitials : sCountryCodesAll) {
            int localizedCountryNameResId = resources.getIdentifier(countryInitials, "string", packageName);
            
            CountryCode country = new CountryCode(
                    context.getString(localizedCountryNameResId),
                    countryInitials,
                    phoneUtil.getCountryCodeForRegion(countryInitials));
            
            // LogIt.d(CountryCode.class, "Add country to list", country);
            sCountryListAll.add(country);
        }
        
        LogIt.d(CountryListActivity.class, "Created country lists",
                sCountryListPopular.size(), sCountryListAll.size());
    }

	Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (msg == null) {
				LogIt.w(CountryListActivity.class,
						"Ignore null message received by handler");
				return;
			}

			switch (msg.what) {
			case UPDATE_SEARCH_MESSAGE:
				String terms = (String) msg.obj;
				LogIt.d(CountryListActivity.class, "UPDATE_SEARCH_MESSAGE",
						terms);
				adapter.deleteAllItems();
				displayCountries(doSearch(terms, countries), terms);
				break;
			default:
				LogIt.w(CountryListActivity.class,
						"Unexpected message received by handler", msg.what);
				break;
			}
		}
	};

	public static List<CountryCode> getCountryList() {
	    return sCountryListAll;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.search_countries);

		countryList = (ListView) findViewById(R.id.country_list);

		searchBox = (EditText) findViewById(R.id.search_box);

		String title = getIntent().getStringExtra(EXTRA_INPUT_TITLE);
		LogIt.d(this, "Title", title);
		
		if (!StringUtil.isEmpty(title)) {
		    this.setTitle(title);
		}
		
		String legend = getIntent().getStringExtra(EXTRA_INPUT_LEGEND_TEXT);		
		LogIt.d(this, "Legend", legend);
		
		if (!StringUtil.isEmpty(legend)) {  
		    // The layout defaults to GONE
		    LinearLayout container = (LinearLayout) findViewById(R.id.legend_container);
		    container.setVisibility(View.VISIBLE);
		    
		    TextView legendView = (TextView) findViewById(R.id.legend);
		    legendView.setText(legend);
		}
		
		adapter = new PhoneAdapter(this);

		loadCountries();

		countryList.setAdapter(adapter);

		countryList.setOnItemClickListener(new ListItemClickListener());

		searchBox.addTextChangedListener(new SearchTextWatcher());

	}

	public void onSearch(View view) {
		String terms = searchBox.getText().toString();
		LogIt.d(this, "User clicked in search icon");
		if (!StringUtil.isEmpty(terms)) {
			adapter.deleteAllItems();
			displayCountries(doSearch(terms, countries), terms);
		}
	}

	private void loadCountries() {

		adapter.deleteAllItems();

		countries = CountryListActivity.getCountryList();
		displayAlphaList();
		setAlphabeticallySortedAdapter(countries, adapter);
	}

	private void displayAlphaList() {
		for (CountryCode country : sCountryListPopular) {
			adapter.addItem(country);
		}
	}

	private void displayCountries(List<CountryCode> countries, String terms) {
		adapter.deleteAllItems();
		if (terms.length() == 0) {
			displayAlphaList();
		}
		setAlphabeticallySortedAdapter(countries, adapter);
	}

	private class ListItemClickListener implements OnItemClickListener {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int position,
				long arg3) {

			CountryCode selectecCountry = (CountryCode) adapter
					.getItem(position);
            LogIt.user(CountryListActivity.class, "Selected country",
                    selectecCountry.getCountryName(),
                    selectecCountry.getCountryCode());

		    Intent intent = new Intent();
		    intent.putExtra(MessageMeConstants.EXTRA_COUNTRY_NAME,
		            selectecCountry.getCountryName());
		    intent.putExtra(MessageMeConstants.EXTRA_CONTRY_CODE,
		            String.valueOf(selectecCountry.getCountryCode()));
		    intent.putExtra(MessageMeConstants.EXTRA_COUNTRY_INITIALS,
		            selectecCountry.getCountryShortName());
		    
		    setResult(RESULT_OK, intent);
			finish();

		}

	}

	private void setAlphabeticallySortedAdapter(List<CountryCode> list,
			PhoneAdapter adapter) {

		for (int i = 0; i < list.size(); i++) {

			if (i == 0) {
				adapter.addSeparator(list.get(i));
			}

			adapter.addItem(list.get(i));

			if ((i < list.size() - 1)
					&& (!list
							.get(i + 1)
							.getCountryInitial()
							.toUpperCase()
							.equals(list.get(i).getCountryInitial()
									.toUpperCase()))) {
				adapter.addSeparator(list.get(i + 1));
			}

		}

	}

	public List<CountryCode> doSearch(String terms, List<CountryCode> list) {

		List<CountryCode> sortedCountries = new ArrayList<CountryCode>();
		int textLength = 0;

		textLength = terms.length();
		sortedCountries.clear();

		for (int i = 0; i < list.size(); i++) {
			if (textLength <= list.get(i).getCountryName().length()) {

				if (terms.toString().equalsIgnoreCase(
						(String) list.get(i).getCountryName()
								.subSequence(0, textLength))) {
					sortedCountries.add(list.get(i));
				}
			}
		}
		return sortedCountries;
	}

	private class SearchTextWatcher implements TextWatcher {

		@Override
		public void afterTextChanged(Editable arg0) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {

			mHandler.removeMessages(UPDATE_SEARCH_MESSAGE); // start counting
															// again
			mHandler.sendMessageDelayed(
					mHandler.obtainMessage(UPDATE_SEARCH_MESSAGE, s.toString()),
					TEXT_CHANGE_THRESHOLD);

		}
	}

	private class PhoneAdapter extends BaseAdapter {

		private static final int TYPE_ITEM = 0;

		private static final int TYPE_SEPARATOR = 1;

		private static final int TYPE_MAX_COUNT = TYPE_SEPARATOR + 1;

		private ViewHolder holder;

		private LayoutInflater inflater;

		private List<CountryCode> data = new ArrayList<CountryCode>();

		private TreeSet<Integer> mSeparatorsSet = new TreeSet<Integer>();

		public PhoneAdapter(Context context) {

			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		public void deleteAllItems() {
			data.clear();
			mSeparatorsSet.clear();
			notifyDataSetChanged();
		}

		public void addItem(CountryCode item) {
			data.add(item);
			notifyDataSetChanged();
		}

		public void addSeparator(CountryCode item) {
			data.add(item);

			mSeparatorsSet.add(data.size() - 1);
			notifyDataSetChanged();
		}

		@Override
		public int getItemViewType(int position) {
			return mSeparatorsSet.contains(position) ? TYPE_SEPARATOR
					: TYPE_ITEM;
		}

		@Override
		public int getViewTypeCount() {
			return TYPE_MAX_COUNT;
		}

		@Override
		public int getCount() {
			return data.size();
		}

		@Override
		public Object getItem(int position) {
			return data.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			CountryCode country = (CountryCode) getItem(position);

			int type = getItemViewType(position);

			if (convertView == null) {
				holder = new ViewHolder();

				switch (type) {
				case TYPE_ITEM:
					convertView = inflater.inflate(
							R.layout.country_list_content, null);
					holder.countryNumber = (TextView) convertView
							.findViewById(R.id.country_code);
					holder.countryName = (TextView) convertView
							.findViewById(R.id.country_name);
					convertView.setTag(holder);
					break;

				case TYPE_SEPARATOR:
					convertView = inflater.inflate(
							R.layout.layout_listview_separator, null);
					holder.separator = (TextView) convertView
							.findViewById(R.id.separator);
					break;
				}
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder = getMessageContent(country, type);
			return convertView;

		}

		private ViewHolder getMessageContent(CountryCode country, int type) {
			switch (type) {
			case TYPE_ITEM:
				holder.countryNumber.setText("+"
						+ String.valueOf(country.getCountryCode()));
				holder.countryName.setText(country.getCountryName());

				break;

			case TYPE_SEPARATOR:
				holder.separator.setText(country.getCountryInitial()
						.toUpperCase());
				break;
			}

			return holder;

		}

	}

	public static class ViewHolder {

		public TextView countryNumber;

		public TextView countryName;

		public TextView separator;
	}

}
