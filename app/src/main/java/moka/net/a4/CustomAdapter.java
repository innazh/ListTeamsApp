package moka.net.a4;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class CustomAdapter  extends ArrayAdapter<String> {
    private Activity context;
    private String[] city;
    private String[] name;

    public CustomAdapter(Activity contextPar, String[] city, String[] name) {
        super(contextPar, R.layout.individual_item, city);
        this.context = contextPar;
        this.city = city;
        this.name = name;
    }

    public View getView(int position, View view, ViewGroup parent) {
        LayoutInflater inflater = this.context.getLayoutInflater();

        if (view == null) {
            view = inflater.inflate(R.layout.individual_item, parent, false);
        }

        TextView tvCity = view.findViewById(R.id.cityTv);
        TextView tvName = view.findViewById(R.id.nameTv);
        tvCity.setText(this.city[position]);
        tvName.setText(this.name[position]);

        return view;
    }
}
