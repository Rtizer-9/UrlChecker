package com.trianguloy.urlchecker.modules.list;

import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.trianguloy.urlchecker.R;
import com.trianguloy.urlchecker.activities.ConfigActivity;
import com.trianguloy.urlchecker.dialogs.MainDialog;
import com.trianguloy.urlchecker.modules.AModuleConfig;
import com.trianguloy.urlchecker.modules.AModuleData;
import com.trianguloy.urlchecker.modules.AModuleDialog;
import com.trianguloy.urlchecker.modules.DescriptionConfig;
import com.trianguloy.urlchecker.url.UrlData;
import com.trianguloy.urlchecker.utilities.AndroidUtils;
import com.trianguloy.urlchecker.utilities.Inflater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This module removes queries "?foo=bar" from an url
 * Originally made by PabloOQ
 */
public class RemoveQueriesModule extends AModuleData {

    @Override
    public String getId() {
        return "removeQueries";
    }

    @Override
    public int getName() {
        return R.string.mRemove_name;
    }

    @Override
    public AModuleDialog getDialog(MainDialog cntx) {
        return new RemoveQueriesDialog(cntx);
    }

    @Override
    public AModuleConfig getConfig(ConfigActivity cntx) {
        return new DescriptionConfig(R.string.mRemove_desc);
    }
}

class RemoveQueriesDialog extends AModuleDialog implements View.OnClickListener {

    private TextView info;
    private Button remove;
    private LinearLayout box;

    public RemoveQueriesDialog(MainDialog dialog) {
        super(dialog);
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_removequeries;
    }

    @Override
    public void onInitialize(View views) {
        info = views.findViewById(R.id.text);
        remove = views.findViewById(R.id.fix);
        box = views.findViewById(R.id.box);

        // expand queries
        info.setOnClickListener(v -> {
            box.setVisibility(box.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
            updateMoreIndicator();
        });

        remove.setOnClickListener(this);
    }

    @Override
    public void onNewUrl(UrlData urlData) {
        // initialize
        box.removeAllViews();

        // parse
        UrlParts parts = new UrlParts(urlData.url);

        if (parts.getQueries() == 0) {
            // no queries present, nothing to notify
            info.setText(R.string.mRemove_noQueries);
            AndroidUtils.clearRoundedColor(info);
            remove.setEnabled(false); // disable the remove button
        } else {
            // queries present, notify
            SpannableStringBuilder text = new SpannableStringBuilder(
                    parts.getQueries() == 1
                            ? getActivity().getString(R.string.mRemove_found1) // 1 query
                            : getActivity().getString(R.string.mRemove_found, parts.getQueries()) // 2+ queries
            );
            // mark the text as clickable (but the click does nothing, it is managed when clicked the view)
            text.setSpan(new ClickableSpan() {
                @Override
                public void onClick(View ignored) {
                }
            }, 0, text.length(), 0);
            info.setText(text);
            AndroidUtils.setRoundedColor(R.color.warning, info, getActivity());
            remove.setEnabled(true); // enable the remove all button

            // for each query, create a button
            for (int i = 0; i < parts.getQueries(); i++) {
                Button queryRemover = Inflater.inflate(R.layout.extra_remove_button, box, getActivity());
                queryRemover.setTag(i); // will remove this i query when clicked
                queryRemover.setOnClickListener(this);
                String queryName = parts.getQueryName(i);
                queryRemover.setText(queryName.isEmpty()
                        // if no name
                        ? getActivity().getString(R.string.mRemove_empty)
                        // with name
                        : getActivity().getString(R.string.mRemove_one, queryName)
                );
            }
        }

        // update
        updateMoreIndicator();
    }

    @Override
    public void onClick(View v) {
        UrlParts parts = new UrlParts(getUrl());

        // remove all queries (no tag) or a specific one
        Integer tag = (Integer) v.getTag();
        parts.removeQuery(tag == null ? -1 : tag);

        // join and set
        setUrl(parts.getUrl());
    }

    /**
     * Sets the 'more' indicator.
     */
    private void updateMoreIndicator() {
        info.setCompoundDrawablesWithIntrinsicBounds(
                box.getChildCount() == 0 ? 0
                        : box.getVisibility() == View.VISIBLE ? R.drawable.expanded
                        : R.drawable.collapsed,
                0, 0, 0);
    }

    /**
     * Manages the splitting, removing and merging of queries
     */
    private static class UrlParts {
        private final String preQuery; // "http://google.com"
        private final List<String> queries = new ArrayList<>(); // ["ref=foo","bar"]
        private final String postQuery; // "#start"

        /**
         * Prepares a url and extracts its queries
         */
        public UrlParts(String url) {
            // an uri is defined as [scheme:][//authority][path][?query][#fragment]
            // we need to find a '?' followed by anything except a '#'
            // this allows us to work with any string, even with non-standard or malformed uris
            int iStart = url.indexOf("?"); // position of '?' (-1 if not present)
            int iEnd = url.indexOf("#", iStart + 1);
            iEnd = iEnd == -1 ? url.length() : iEnd; // position of '#' (end of string if not present)

            // add part until '?' or until postQuery if not present
            preQuery = url.substring(0, iStart != -1 ? iStart : iEnd);
            // add queries if any
            if (iStart != -1) {
                queries.addAll(splitFix(url.substring(iStart + 1, iEnd), "&"));
            }
            // add part after queries (empty if not present)
            postQuery = url.substring(iEnd);
        }

        /**
         * Joins the url back into a full string
         */
        public String getUrl() {
            StringBuilder sb = new StringBuilder(preQuery);
            // first query after '?', the rest after '&'
            for (int i = 0; i < queries.size(); ++i) sb.append(i == 0 ? "?" : "&").append(queries.get(i));
            sb.append(postQuery);
            return sb.toString();
        }

        /**
         * returns the number of queries present
         */
        public int getQueries() {
            return queries.size();
        }

        /**
         * Returns the name of the query
         */
        public String getQueryName(int i) {
            return queries.get(i).split("=")[0];
        }

        /**
         * Removes a query by its index i, or all if -1
         */
        public void removeQuery(int i) {
            if (i == -1) {
                // remove all queries
                queries.clear();
            } else {
                // remove that query
                queries.remove(i);
            }
        }
    }

    /**
     * {@link String#split(String)} won't return the last element if it's empty.
     * This function does. And it returns a list instead of an array.
     * Everything else is the same.
     * Note: regexp must not match '#', it does change this function
     */
    private static List<String> splitFix(String string, String regexp) {
        // split with an extra char
        String[] parts = (string + "#").split(regexp);
        // remove the extra char
        parts[parts.length - 1] = parts[parts.length - 1].substring(0, parts[parts.length - 1].length() - 1);
        return Arrays.asList(parts);
    }
}
