package com.example.ioandroid.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView
import com.example.ioandroid.models.DataEntry

class ExpandableListAdapter(private val context: Context, private val entries: MutableList<DataEntry>) :
    BaseExpandableListAdapter() {

    override fun getGroupCount(): Int {
        return entries.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        // You can customize this based on your data model
        return 1
    }

    override fun getGroup(groupPosition: Int): Any {
        return entries[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        // You can customize this based on your data model
        return "Additional details here"
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        // Implement the layout for the group view (collapsed)
        // You can use a custom layout or android.R.layout.simple_expandable_list_item_1
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(android.R.layout.simple_expandable_list_item_1, null)
        val text = view.findViewById<TextView>(android.R.id.text1)
        text.text = entries[groupPosition].label + " " + groupPosition
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        // Implement the layout for the child view (expanded)
        // You can use a custom layout or android.R.layout.simple_expandable_list_item_2
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(android.R.layout.simple_expandable_list_item_2, null)
        val text1 = view.findViewById<TextView>(android.R.id.text1)
        val text2 = view.findViewById<TextView>(android.R.id.text2)
        text1.text = "Location details: ${entries[groupPosition]}"
        text2.text = "Additional details: ${getChild(groupPosition, childPosition)}"
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return false
    }

    fun removeGroup(groupPosition: Int) {
        entries.removeAt(groupPosition)
        notifyDataSetChanged()
    }
}
