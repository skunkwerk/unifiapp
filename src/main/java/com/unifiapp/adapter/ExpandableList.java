package com.unifiapp.adapter;

import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.unifiapp.R;

import de.hdodenhof.circleimageview.CircleImageView;

public class ExpandableList extends BaseExpandableListAdapter
{

    private final SparseArray<Group> groups;
    public LayoutInflater inflater;

    public ExpandableList(LayoutInflater inflater, SparseArray<Group> groups)
    {
        this.groups = groups;
        this.inflater = inflater;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition)
    {
        return groups.get(groupPosition).children.get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition)
    {
        return 0;
    }

    @Override
    public View getChildView(int groupPosition, final int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent)
    {
        final String children = (String) getChild(groupPosition, childPosition);
        TextView text = null;
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.leaderboard_listview_child, null);
        }
        text = (TextView) convertView.findViewById(R.id.textView1);
        text.setText(children);
        convertView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Log.d("in ExpandableList","clicked");
            }
        });
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition)
    {
        return groups.get(groupPosition).children.size();
    }

    @Override
    public Object getGroup(int groupPosition)
    {
        return groups.get(groupPosition);
    }

    @Override
    public int getGroupCount()
    {
        return groups.size();
    }

    @Override
    public void onGroupCollapsed(int groupPosition)
    {
        super.onGroupCollapsed(groupPosition);
    }

    @Override
    public void onGroupExpanded(int groupPosition)
    {
        super.onGroupExpanded(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition)
    {
        return 0;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent)
    {
        if (convertView == null)
        {
            convertView = inflater.inflate(R.layout.leaderboard_listview_parent, null);
        }
        CheckedTextView checkedTextView = (CheckedTextView) convertView.findViewById(R.id.textViewParent);
        TextView rightTextView = (TextView) convertView.findViewById(R.id.right_text);
        ImageView imgView = (CircleImageView) convertView.findViewById(R.id.profile_picture);
        View border = (View) convertView.findViewById(R.id.leaderboard_item_border);
        Group group = (Group) getGroup(groupPosition);

        if(group!=null && group.profile_picture_url!=null)
        {
            Picasso.with(inflater.getContext()).load(group.profile_picture_url).into(imgView);
            imgView.setVisibility(View.VISIBLE);//when scroll back up
        }
        else
        {
            imgView.setVisibility(View.INVISIBLE);
        }
        if(group!=null && group.string!=null)
        {
            checkedTextView.setText(group.string);
        }
        if(group!=null && group.points!=null)
        {
            rightTextView.setText(group.points);
        }
        else
        {
            rightTextView.setText("");
        }
        checkedTextView.setChecked(isExpanded);

        //if the group is a placeholder for a dot, hide the border
        //their images and borders disappear when scroll back up
        if (group!=null && group.placeholder==true)
        {
            border.setVisibility(View.INVISIBLE);
            checkedTextView.setGravity(Gravity.CENTER);
        }
        else
        {
            border.setVisibility(View.VISIBLE);//when scroll back up
            checkedTextView.setGravity(Gravity.LEFT);
        }
        return convertView;
    }

    @Override
    public boolean hasStableIds()
    {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition)
    {
        return false;
    }
}