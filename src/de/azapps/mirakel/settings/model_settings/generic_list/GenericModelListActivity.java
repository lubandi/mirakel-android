/*******************************************************************************
 * Mirakel is an Android App for managing your ToDo-Lists
 *
 *   Copyright (c) 2013-2014 Anatolij Zelenin, Georg Semmler.
 *
 *       This program is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       any later version.
 *
 *       This program is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU General Public License for more details.
 *
 *       You should have received a copy of the GNU General Public License
 *       along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package de.azapps.mirakel.settings.model_settings.generic_list;


import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.google.common.base.Optional;

import java.util.ArrayList;
import java.util.List;

import de.azapps.mirakel.ThemeManager;
import de.azapps.mirakel.adapter.OnItemClickedListener;
import de.azapps.mirakel.adapter.SimpleModelAdapter;
import de.azapps.mirakel.helper.MirakelCommonPreferences;
import de.azapps.mirakel.model.IGenericElementInterface;
import de.azapps.mirakel.settings.R;
import de.azapps.mirakel.settings.SettingsHelper;

import static com.google.common.base.Optional.absent;

public abstract class GenericModelListActivity<T extends IGenericElementInterface> extends
    ActionBarActivity implements  GenericModelListFragment.Callbacks, OnItemClickedListener<T> {

    private static final int RESULT_ITEM = 10;

    private FrameLayout mDetailContainer;
    private boolean mTwoPane;
    private List<T> backstack = new ArrayList<>();

    @Nullable
    private Cursor query = null;

    protected abstract boolean isSupport();

    @NonNull
    protected Optional<android.app.Fragment> getDetailFragment(final @NonNull T item) {
        return absent();
    }

    @NonNull
    protected  Optional<android.support.v4.app.Fragment> getSupportDetailFragment(
        final @NonNull T item) {
        return absent();
    }

    @NonNull
    protected abstract Class<? extends GenericModelListActivity> getSelf();


    private Bundle getDetailArguments(T item) {
        final Bundle arguments = new Bundle();
        arguments.putParcelable(GenericModelDetailFragment.ARG_ITEM, item);
        return arguments;
    }

    private Optional<android.app.Fragment> instanceDetail(final @NonNull T item) {
        final Optional<android.app.Fragment> fragment = getDetailFragment(item);
        if (!fragment.isPresent()) {
            return absent();
        }
        fragment.get().setArguments(getDetailArguments(item));
        return fragment;
    }


    private Optional<android.support.v4.app.Fragment> instanceSupportDetail(final @NonNull T item) {
        final Optional<android.support.v4.app.Fragment> fragment = getSupportDetailFragment(item);
        if (!fragment.isPresent()) {
            return absent();
        }
        fragment.get().setArguments(getDetailArguments(item));
        return fragment;
    }

    @NonNull
    protected abstract T getDefaultItem();

    /**
     * Call selectItem in your implementation after creating
     * @param ctx
     */
    protected abstract void createItem(final @NonNull Context ctx);

    protected abstract String getTextTitle();


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        ThemeManager.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generic_model_twopane);
        setUpActionBar();
        // Show the Up button in the action bar.
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mDetailContainer = (FrameLayout)findViewById(R.id.generic_model_detail_container);

        mTwoPane = MirakelCommonPreferences.isTablet();
        mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
        final FragmentTransaction transaction = getSupportFragmentManager().beginTransaction().replace(
                R.id.generic_model_list_container, new GenericModelListFragment());
        if (mTwoPane) {
            if (!isSupport()) {
                final Optional<Fragment> f = instanceDetail(getDefaultItem());
                if (f.isPresent()) {
                    getFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                            f.get()).commit();
                }
            } else {
                final Optional<android.support.v4.app.Fragment> f = instanceSupportDetail(getDefaultItem());
                if (f.isPresent()) {
                    transaction.replace(R.id.generic_model_detail_container, f.get());
                }
            }
        }
        transaction.commit();
    }

    protected void setUpActionBar() {
        final Toolbar bar = (Toolbar) findViewById(R.id.actionbar);
        bar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(final MenuItem menuItem) {
                return onOptionsItemSelected(menuItem);
            }
        });
        bar.setBackgroundColor(ThemeManager.getColor(R.attr.colorPrimary));
        bar.setVisibility(View.VISIBLE);
        setSupportActionBar(bar);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onConfigurationChanged(final Configuration newConfig) {
        T oldItem = null;
        final GenericModelDetailFragment<T> frag = (GenericModelDetailFragment<T>)
                getFragmentManager().findFragmentById(R.id.speciallist_detail_container);
        if (frag != null) {
            oldItem = frag.getItem();
        }
        super.onConfigurationChanged(newConfig);
        if (mTwoPane != MirakelCommonPreferences.isTablet()) {
            mTwoPane = MirakelCommonPreferences.isTablet();
            mDetailContainer.setVisibility(mTwoPane ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();
            if (mTwoPane) {
                if (!isSupport()) {
                    final Optional<Fragment> f = instanceDetail(getDefaultItem());
                    if (f.isPresent()) {
                        getFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                f.get()).commitAllowingStateLoss();
                    }
                } else {
                    final Optional<android.support.v4.app.Fragment> f = instanceSupportDetail(getDefaultItem());
                    if (f.isPresent()) {
                        getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                f.get()).commitAllowingStateLoss();
                    }
                }
            } else {
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle(getTextTitle());
                }
                if (oldItem != null) {
                    onItemSelected(oldItem);
                }
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        if (requestCode == RESULT_ITEM) {
            switch (resultCode) {
            case GenericModelDetailActivity.NEED_UPDATE:
                updateList();
                break;
            case GenericModelDetailActivity.SWITCH_LAYOUT:
                if (mTwoPane && (data != null)) {
                    if (!isSupport()) {
                        final Optional<Fragment> f = instanceDetail((T) data.getParcelableExtra(
                                                         GenericModelDetailFragment.ARG_ITEM));
                        if (f.isPresent()) {
                            getFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                    f.get()).commitAllowingStateLoss();
                        }
                    } else {
                        final Optional<android.support.v4.app.Fragment> f = instanceSupportDetail((
                                    T) data.getParcelableExtra(
                                    GenericModelDetailFragment.ARG_ITEM));
                        if (f.isPresent()) {
                            getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                                    f.get()).commitAllowingStateLoss();
                        }
                    }
                }
            }
        } else if (SettingsHelper.handleActivityResult(requestCode, resultCode, data, this)) {
            finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void updateList() {
        ((GenericModelListFragment)getSupportFragmentManager().findFragmentById(
             R.id.generic_model_list_container)).reload();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * Callback method from {@link GenericModelListFragment.Callbacks}
     * indicating that the item with the given ID was selected.
     */
    @Override
    public void onItemSelected(final @NonNull T item) {
        backstack.add(item);
        final Optional<Fragment> fragment = instanceDetail(item);
        final Optional<android.support.v4.app.Fragment> supportFragment = instanceSupportDetail(item);
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            if (!isSupport() && fragment.isPresent()) {
                getFragmentManager().beginTransaction()
                .replace(R.id.generic_model_detail_container, fragment.get()).commit();
            } else if (isSupport() && supportFragment.isPresent()) {
                getSupportFragmentManager().beginTransaction().replace(R.id.generic_model_detail_container,
                        supportFragment.get()).commit();
            }
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            final Intent detailIntent = new Intent(this, GenericModelDetailActivity.class);
            boolean startNew = false;
            detailIntent.putExtra(GenericModelDetailFragment.ARG_ITEM, item);
            if (!isSupport() && fragment.isPresent()) {
                detailIntent.putExtra(GenericModelDetailActivity.FRAGMENT,
                                      ((Object) fragment.get()).getClass());
                startNew = true;
            } else if (isSupport() && supportFragment.isPresent()) {
                detailIntent.putExtra(GenericModelDetailActivity.FRAGMENT,
                                      ((Object) supportFragment.get()).getClass());
                startNew = true;
            }
            if (startNew) {
                detailIntent.putExtra(GenericModelDetailActivity.BACK_ACTIVITY, getSelf());
                startActivityForResult(detailIntent, RESULT_ITEM);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onBackPressed() {
        if (backstack.size() > 1) {
            backstack.remove(backstack.size() - 1);
            onItemSelected(backstack.get(backstack.size() - 1));
            backstack.remove(backstack.size() - 1);
        } else {
            super.onBackPressed();
        }
    }

    @NonNull
    @Override
    public RecyclerView.LayoutManager getLayoutManager(final @NonNull Context ctx) {
        return new LinearLayoutManager(ctx);
    }

    public void addItem() {
        createItem(this);
        updateList();
    }

    @Override
    public boolean hasFab() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeCursor();
    }

    private void closeCursor() {
        if (query != null) {
            query.close();
        }
    }

    @Nullable
    @Override
    public RecyclerView.Adapter getAdapter() {
        closeCursor();
        query = getQuery();
        if (query != null) {
            return new SimpleModelAdapter<>(this, query, getItemClass(), this);
        }
        return null;
    }

    @Nullable
    protected Class<T> getItemClass() {
        return null;
    }

    @Nullable
    protected Cursor getQuery() {
        return null;
    }
}
