import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import xyz.hyli.connect.utils.PackageUtils;

public class AppListAdapter extends ArrayAdapter<String> {
    private Context context;
    private List<PackageUtils.AppInfo> appList;

    public AppListAdapter(Context context, List<PackageUtils.AppInfo> appList) {
        super(context, 0, new String[appList.size()]);
        this.context = context;
        this.appList = appList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
        }

        PackageManager packageManager = context.getPackageManager();
        PackageUtils.AppInfo appInfo = appList.get(position);

        // 获取应用的图标和名称
        Drawable appIcon = appInfo.getAppIcon();
        String appName = appInfo.getAppName();
        String packageName = appInfo.getPackageName();
        String mainActivityName = appInfo.getMainActivityName();
        Log.i("AppListAdapter", "packageName: " + packageName);

        // 显示应用的图标和名称
        if (appIcon == null) {
            appIcon = context.getDrawable(android.R.drawable.sym_def_app_icon);
        }
        if (appName == null) {
            appName = packageName;
        }
        ImageView iconView = convertView.findViewById(android.R.id.icon);
        TextView nameView = convertView.findViewById(android.R.id.text1);
//        iconView.setImageDrawable(appIcon);
        nameView.setText(appName);


        return convertView;
    }
}
