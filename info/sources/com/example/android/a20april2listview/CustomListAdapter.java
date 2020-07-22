package com.example.android.a20april2listview;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/* compiled from: MainActivity */
class CustomListAdapter extends ArrayAdapter<String> {
    private Activity context;
    private String[] itemname0a;
    private String[] itemname1a;
    private String[] itemname2a;

    CustomListAdapter(Activity contextPar, String[] itemname0, String[] itemname1, String[] itemname2) {
        super(contextPar, R.layout.da_item, itemname2);
        this.context = contextPar;
        this.itemname0a = itemname0;
        this.itemname1a = itemname1;
        this.itemname2a = itemname2;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = this.context.getLayoutInflater();
        if (view == null) {
            view = inflater.inflate(R.layout.da_item, parent, false);
        }
        ((TextView) view.findViewById(R.id.da_itemTV0)).setText(this.itemname0a[position]);
        ((TextView) view.findViewById(R.id.da_itemTV)).setText(this.itemname1a[position]);
        ((TextView) view.findViewById(R.id.da_item2)).setText(this.itemname2a[position]);
        return view;
    }
}
