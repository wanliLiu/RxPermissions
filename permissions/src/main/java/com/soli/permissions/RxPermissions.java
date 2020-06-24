/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.soli.permissions;

import android.annotation.TargetApi;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.subjects.PublishSubject;

/*
 * @author soli
 * @Time 2018/5/23 22:27
 */
public class RxPermissions implements LifecycleEventObserver {

    private final Object TRIGGER = new Object();
    static final String TAG = "RxPermissions";
    private RxPermissionsFragment mRxPermissionsFragment;
    private FragmentManager manager;

    /**
     * @param activity
     */
    public RxPermissions(@NonNull AppCompatActivity activity) {
        addLifeCycleListener(activity);
        mRxPermissionsFragment = getRxPermissionsFragment(((AppCompatActivity) activity).getSupportFragmentManager());
    }

    /**
     * @param fragment
     */
    public RxPermissions(Fragment fragment) {
        addLifeCycleListener(fragment);
        mRxPermissionsFragment = getRxPermissionsFragment(fragment.getChildFragmentManager());
    }

    /**
     * @param source
     */
    private void addLifeCycleListener(LifecycleOwner source) {
        source.getLifecycle().addObserver(this);
    }

    @Override
    public void onStateChanged(@androidx.annotation.NonNull LifecycleOwner source, @androidx.annotation.NonNull Lifecycle.Event event) {
        Log.e("onStateChanged", source.getClass().getSimpleName() + "--->" + event.name());
        if (event == Lifecycle.Event.ON_DESTROY) {
            source.getLifecycle().removeObserver(this);
            manager = null;
            mRxPermissionsFragment = null;
        }
    }

    /**
     * @param mManger
     * @return
     */
    private RxPermissionsFragment getRxPermissionsFragment(FragmentManager mManger) {
        manager = mManger;
        RxPermissionsFragment rxPermissionsFragment = findRxPermissionsFragment(manager);
        boolean isNewInstance = rxPermissionsFragment == null;
        if (isNewInstance) {
            rxPermissionsFragment = new RxPermissionsFragment();
            manager
                    .beginTransaction()
                    .add(rxPermissionsFragment, TAG)
                    .commitAllowingStateLoss();
            manager.executePendingTransactions();
        }
        return rxPermissionsFragment;
    }

    /**
     * @param manager
     * @return
     */
    private RxPermissionsFragment findRxPermissionsFragment(FragmentManager manager) {
        return (RxPermissionsFragment) manager.findFragmentByTag(TAG);
    }

    /**
     * @param logging
     */
    public void setLogging(boolean logging) {
        mRxPermissionsFragment.setLogging(logging);
    }

    /**
     * Map emitted items from the source observable into {@code true} if permissions in parameters
     * are granted, or {@code false} if not.
     * <p>
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    public <T> ObservableTransformer<T, Boolean> ensure(final String... permissions) {
        return upstream -> request(upstream, permissions)
                // Transform Observable<Permission> to Observable<Boolean>
                .buffer(permissions.length)
                .flatMap((Function<List<Permission>, ObservableSource<Boolean>>) permissions1 -> {
                    if (permissions1.isEmpty()) {
                        // Occurs during orientation change, when the subject receives onComplete.
                        // In that case we don't want to propagate that empty list to the
                        // subscriber, only the onComplete.
                        return Observable.empty();
                    }
                    // Return true if all permissions are granted.
                    for (Permission p : permissions1) {
                        if (!p.granted) {
                            return Observable.just(false);
                        }
                    }
                    return Observable.just(true);
                });
    }

    /**
     * Map emitted items from the source observable into {@link Permission} objects for each
     * permission in parameters.
     * <p>
     * If one or several permissions have never been requested, invoke the related framework method
     * to ask the user if he allows the permissions.
     */
    public <T> ObservableTransformer<T, Permission> ensureEach(final String... permissions) {
        return o -> request(o, permissions);
    }

    /**
     * Request permissions immediately, <b>must be invoked during initialization phase
     * of your application</b>.
     */
    public Observable<Boolean> request(final String... permissions) {
        return Observable.just(TRIGGER).compose(ensure(permissions));
    }

    /**
     * Request permissions immediately, <b>must be invoked during initialization phase
     * of your application</b>.
     */
    public Observable<Permission> requestEach(final String... permissions) {
        return Observable.just(TRIGGER).compose(ensureEach(permissions));
    }

    /**
     * @param trigger
     * @param permissions
     * @return
     */
    private Observable<Permission> request(final Observable<?> trigger,
                                           final String... permissions) {
        if (permissions == null || permissions.length == 0) {
            throw new IllegalArgumentException("RxPermissions.request/requestEach requires at least one input permission");
        }
        return oneOf(trigger, pending(permissions))
                .flatMap((Function<Object, Observable<Permission>>) o -> requestImplementation(permissions));
    }

    private Observable<?> pending(final String... permissions) {
        for (String p : permissions) {
            if (!mRxPermissionsFragment.containsByPermission(p)) {
                return Observable.empty();
            }
        }
        return Observable.just(TRIGGER);
    }

    private Observable<?> oneOf(Observable<?> trigger, Observable<?> pending) {
        if (trigger == null) {
            return Observable.just(TRIGGER);
        }
        return Observable.merge(trigger, pending);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private Observable<Permission> requestImplementation(final String... permissions) {

        List<Observable<Permission>> list = new ArrayList<>(permissions.length);
        List<String> unrequestedPermissions = new ArrayList<>();

        // In case of multiple permissions, we create an Observable for each of them.
        // At the end, the observables are combined to have a unique response.
        for (String permission : permissions) {
            mRxPermissionsFragment.log("Requesting permission " + permission);

            if (isGranted(permission)) {
                // Already granted, or not Android M
                // Return a granted Permission object.
                list.add(Observable.just(new Permission(permission, true, false)));
                continue;
            }

            if (isRevoked(permission)) {
                // Revoked by a policy, return a denied Permission object.
                list.add(Observable.just(new Permission(permission, false, false)));
                continue;
            }

            PublishSubject<Permission> subject = mRxPermissionsFragment.getSubjectByPermission(permission);
            // Create a new subject if not exists
            if (subject == null) {
                unrequestedPermissions.add(permission);
                subject = PublishSubject.create();
                mRxPermissionsFragment.setSubjectForPermission(permission, subject);
            }

            list.add(subject);
        }

        if (!unrequestedPermissions.isEmpty()) {
            String[] unrequestedPermissionsArray = unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]);
            requestPermissionsFromFragment(unrequestedPermissionsArray);
        }

        return Observable.concat(Observable.fromIterable(list));
    }

    /**
     * @param permissions
     */
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissionsFromFragment(String[] permissions) {
        mRxPermissionsFragment.log("requestPermissionsFromFragment " + TextUtils.join(", ", permissions));
        mRxPermissionsFragment.handleRequest(permissions);
    }

    /**
     * Returns true if the permission is already granted.
     * <p>
     * Always true if SDK &lt; 23.
     */
    public boolean isGranted(String permission) {
        return !isMarshmallow() || mRxPermissionsFragment.isGranted(permission);
    }

    /**
     * Returns true if the permission has been revoked by a policy.
     * <p>
     * Always false if SDK &lt; 23.
     */
    public boolean isRevoked(String permission) {
        return isMarshmallow() && mRxPermissionsFragment.isRevoked(permission);
    }

    /**
     * @return
     */
    boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

}
