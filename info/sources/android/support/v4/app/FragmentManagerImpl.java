package android.support.v4.app;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.arch.lifecycle.ViewModelStore;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.content.res.TypedArray;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.support.v4.app.Fragment.SavedState;
import android.support.v4.app.FragmentManager.BackStackEntry;
import android.support.v4.app.FragmentManager.FragmentLifecycleCallbacks;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.util.ArraySet;
import android.support.v4.util.DebugUtils;
import android.support.v4.util.LogWriter;
import android.support.v4.util.Pair;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater.Factory2;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.Transformation;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/* compiled from: FragmentManager */
final class FragmentManagerImpl extends FragmentManager implements Factory2 {
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final int ANIM_DUR = 220;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;
    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    static boolean DEBUG = false;
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    static Field sAnimationListenerField = null;
    SparseArray<Fragment> mActive;
    final ArrayList<Fragment> mAdded = new ArrayList<>();
    ArrayList<Integer> mAvailBackStackIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<OnBackStackChangedListener> mBackStackChangeListeners;
    ArrayList<BackStackRecord> mBackStackIndices;
    FragmentContainer mContainer;
    ArrayList<Fragment> mCreatedMenus;
    int mCurState = 0;
    boolean mDestroyed;
    Runnable mExecCommit = new Runnable() {
        public void run() {
            FragmentManagerImpl.this.execPendingActions();
        }
    };
    boolean mExecutingActions;
    boolean mHavePendingDeferredStart;
    FragmentHostCallback mHost;
    private final CopyOnWriteArrayList<Pair<FragmentLifecycleCallbacks, Boolean>> mLifecycleCallbacks = new CopyOnWriteArrayList<>();
    boolean mNeedMenuInvalidate;
    int mNextFragmentIndex = 0;
    String mNoTransactionsBecause;
    Fragment mParent;
    ArrayList<OpGenerator> mPendingActions;
    ArrayList<StartEnterTransitionListener> mPostponedTransactions;
    Fragment mPrimaryNav;
    FragmentManagerNonConfig mSavedNonConfig;
    SparseArray<Parcelable> mStateArray = null;
    Bundle mStateBundle = null;
    boolean mStateSaved;
    boolean mStopped;
    ArrayList<Fragment> mTmpAddedFragments;
    ArrayList<Boolean> mTmpIsPop;
    ArrayList<BackStackRecord> mTmpRecords;

    /* compiled from: FragmentManager */
    private static class AnimateOnHWLayerIfNeededListener extends AnimationListenerWrapper {
        View mView;

        AnimateOnHWLayerIfNeededListener(View v, AnimationListener listener) {
            super(listener);
            this.mView = v;
        }

        public void onAnimationEnd(Animation animation) {
            if (ViewCompat.isAttachedToWindow(this.mView) || VERSION.SDK_INT >= 24) {
                this.mView.post(new Runnable() {
                    public void run() {
                        AnimateOnHWLayerIfNeededListener.this.mView.setLayerType(0, null);
                    }
                });
            } else {
                this.mView.setLayerType(0, null);
            }
            super.onAnimationEnd(animation);
        }
    }

    /* compiled from: FragmentManager */
    private static class AnimationListenerWrapper implements AnimationListener {
        private final AnimationListener mWrapped;

        private AnimationListenerWrapper(AnimationListener wrapped) {
            this.mWrapped = wrapped;
        }

        public void onAnimationStart(Animation animation) {
            AnimationListener animationListener = this.mWrapped;
            if (animationListener != null) {
                animationListener.onAnimationStart(animation);
            }
        }

        public void onAnimationEnd(Animation animation) {
            AnimationListener animationListener = this.mWrapped;
            if (animationListener != null) {
                animationListener.onAnimationEnd(animation);
            }
        }

        public void onAnimationRepeat(Animation animation) {
            AnimationListener animationListener = this.mWrapped;
            if (animationListener != null) {
                animationListener.onAnimationRepeat(animation);
            }
        }
    }

    /* compiled from: FragmentManager */
    private static class AnimationOrAnimator {
        public final Animation animation;
        public final Animator animator;

        private AnimationOrAnimator(Animation animation2) {
            this.animation = animation2;
            this.animator = null;
            if (animation2 == null) {
                throw new IllegalStateException("Animation cannot be null");
            }
        }

        private AnimationOrAnimator(Animator animator2) {
            this.animation = null;
            this.animator = animator2;
            if (animator2 == null) {
                throw new IllegalStateException("Animator cannot be null");
            }
        }
    }

    /* compiled from: FragmentManager */
    private static class AnimatorOnHWLayerIfNeededListener extends AnimatorListenerAdapter {
        View mView;

        AnimatorOnHWLayerIfNeededListener(View v) {
            this.mView = v;
        }

        public void onAnimationStart(Animator animation) {
            this.mView.setLayerType(2, null);
        }

        public void onAnimationEnd(Animator animation) {
            this.mView.setLayerType(0, null);
            animation.removeListener(this);
        }
    }

    /* compiled from: FragmentManager */
    private static class EndViewTransitionAnimator extends AnimationSet implements Runnable {
        private final View mChild;
        private boolean mEnded;
        private final ViewGroup mParent;
        private boolean mTransitionEnded;

        EndViewTransitionAnimator(Animation animation, ViewGroup parent, View child) {
            super(false);
            this.mParent = parent;
            this.mChild = child;
            addAnimation(animation);
        }

        public boolean getTransformation(long currentTime, Transformation t) {
            if (this.mEnded) {
                return !this.mTransitionEnded;
            }
            if (!super.getTransformation(currentTime, t)) {
                this.mEnded = true;
                OneShotPreDrawListener.add(this.mParent, this);
            }
            return true;
        }

        public boolean getTransformation(long currentTime, Transformation outTransformation, float scale) {
            if (this.mEnded) {
                return !this.mTransitionEnded;
            }
            if (!super.getTransformation(currentTime, outTransformation, scale)) {
                this.mEnded = true;
                OneShotPreDrawListener.add(this.mParent, this);
            }
            return true;
        }

        public void run() {
            this.mParent.endViewTransition(this.mChild);
            this.mTransitionEnded = true;
        }
    }

    /* compiled from: FragmentManager */
    static class FragmentTag {
        public static final int[] Fragment = {16842755, 16842960, 16842961};
        public static final int Fragment_id = 1;
        public static final int Fragment_name = 0;
        public static final int Fragment_tag = 2;

        FragmentTag() {
        }
    }

    /* compiled from: FragmentManager */
    interface OpGenerator {
        boolean generateOps(ArrayList<BackStackRecord> arrayList, ArrayList<Boolean> arrayList2);
    }

    /* compiled from: FragmentManager */
    private class PopBackStackState implements OpGenerator {
        final int mFlags;
        final int mId;
        final String mName;

        PopBackStackState(String name, int id, int flags) {
            this.mName = name;
            this.mId = id;
            this.mFlags = flags;
        }

        public boolean generateOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
            if (FragmentManagerImpl.this.mPrimaryNav != null && this.mId < 0 && this.mName == null) {
                FragmentManager childManager = FragmentManagerImpl.this.mPrimaryNav.peekChildFragmentManager();
                if (childManager != null && childManager.popBackStackImmediate()) {
                    return false;
                }
            }
            return FragmentManagerImpl.this.popBackStackState(records, isRecordPop, this.mName, this.mId, this.mFlags);
        }
    }

    /* compiled from: FragmentManager */
    static class StartEnterTransitionListener implements OnStartEnterTransitionListener {
        /* access modifiers changed from: private */
        public final boolean mIsBack;
        private int mNumPostponed;
        /* access modifiers changed from: private */
        public final BackStackRecord mRecord;

        StartEnterTransitionListener(BackStackRecord record, boolean isBack) {
            this.mIsBack = isBack;
            this.mRecord = record;
        }

        public void onStartEnterTransition() {
            this.mNumPostponed--;
            if (this.mNumPostponed == 0) {
                this.mRecord.mManager.scheduleCommit();
            }
        }

        public void startListening() {
            this.mNumPostponed++;
        }

        public boolean isReady() {
            return this.mNumPostponed == 0;
        }

        public void completeTransaction() {
            boolean z = false;
            boolean canceled = this.mNumPostponed > 0;
            FragmentManagerImpl manager = this.mRecord.mManager;
            int numAdded = manager.mAdded.size();
            for (int i = 0; i < numAdded; i++) {
                Fragment fragment = (Fragment) manager.mAdded.get(i);
                fragment.setOnStartEnterTransitionListener(null);
                if (canceled && fragment.isPostponed()) {
                    fragment.startPostponedEnterTransition();
                }
            }
            FragmentManagerImpl fragmentManagerImpl = this.mRecord.mManager;
            BackStackRecord backStackRecord = this.mRecord;
            boolean z2 = this.mIsBack;
            if (!canceled) {
                z = true;
            }
            fragmentManagerImpl.completeExecute(backStackRecord, z2, z, true);
        }

        public void cancelTransaction() {
            this.mRecord.mManager.completeExecute(this.mRecord, this.mIsBack, false, false);
        }
    }

    FragmentManagerImpl() {
    }

    static boolean modifiesAlpha(AnimationOrAnimator anim) {
        if (anim.animation instanceof AlphaAnimation) {
            return true;
        }
        if (!(anim.animation instanceof AnimationSet)) {
            return modifiesAlpha(anim.animator);
        }
        List<Animation> anims = ((AnimationSet) anim.animation).getAnimations();
        for (int i = 0; i < anims.size(); i++) {
            if (anims.get(i) instanceof AlphaAnimation) {
                return true;
            }
        }
        return false;
    }

    static boolean modifiesAlpha(Animator anim) {
        if (anim == null) {
            return false;
        }
        if (anim instanceof ValueAnimator) {
            PropertyValuesHolder[] values = ((ValueAnimator) anim).getValues();
            for (PropertyValuesHolder propertyName : values) {
                if ("alpha".equals(propertyName.getPropertyName())) {
                    return true;
                }
            }
        } else if (anim instanceof AnimatorSet) {
            List<Animator> animList = ((AnimatorSet) anim).getChildAnimations();
            for (int i = 0; i < animList.size(); i++) {
                if (modifiesAlpha((Animator) animList.get(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, AnimationOrAnimator anim) {
        boolean z = false;
        if (v == null || anim == null) {
            return false;
        }
        if (VERSION.SDK_INT >= 19 && v.getLayerType() == 0 && ViewCompat.hasOverlappingRendering(v) && modifiesAlpha(anim)) {
            z = true;
        }
        return z;
    }

    private void throwException(RuntimeException ex) {
        String message = ex.getMessage();
        String str = TAG;
        Log.e(str, message);
        Log.e(str, "Activity state:");
        PrintWriter pw = new PrintWriter(new LogWriter(str));
        FragmentHostCallback fragmentHostCallback = this.mHost;
        String str2 = "Failed dumping state";
        String str3 = "  ";
        if (fragmentHostCallback != null) {
            try {
                fragmentHostCallback.onDump(str3, null, pw, new String[0]);
            } catch (Exception e) {
                Log.e(str, str2, e);
            }
        } else {
            try {
                dump(str3, null, pw, new String[0]);
            } catch (Exception e2) {
                Log.e(str, str2, e2);
            }
        }
        throw ex;
    }

    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    public boolean executePendingTransactions() {
        boolean updates = execPendingActions();
        forcePostponedTransactions();
        return updates;
    }

    public void popBackStack() {
        enqueueAction(new PopBackStackState(null, -1, 0), false);
    }

    public boolean popBackStackImmediate() {
        checkStateLoss();
        return popBackStackImmediate(null, -1, 0);
    }

    public void popBackStack(String name, int flags) {
        enqueueAction(new PopBackStackState(name, -1, flags), false);
    }

    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        return popBackStackImmediate(name, -1, flags);
    }

    public void popBackStack(int id, int flags) {
        if (id >= 0) {
            enqueueAction(new PopBackStackState(null, id, flags), false);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Bad id: ");
        sb.append(id);
        throw new IllegalArgumentException(sb.toString());
    }

    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        execPendingActions();
        if (id >= 0) {
            return popBackStackImmediate(null, id, flags);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Bad id: ");
        sb.append(id);
        throw new IllegalArgumentException(sb.toString());
    }

    private boolean popBackStackImmediate(String name, int id, int flags) {
        execPendingActions();
        ensureExecReady(true);
        Fragment fragment = this.mPrimaryNav;
        if (fragment != null && id < 0 && name == null) {
            FragmentManager childManager = fragment.peekChildFragmentManager();
            if (childManager != null && childManager.popBackStackImmediate()) {
                return true;
            }
        }
        boolean executePop = popBackStackState(this.mTmpRecords, this.mTmpIsPop, name, id, flags);
        if (executePop) {
            this.mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
            } finally {
                cleanupExec();
            }
        }
        doPendingDeferredStart();
        burpActive();
        return executePop;
    }

    public int getBackStackEntryCount() {
        ArrayList<BackStackRecord> arrayList = this.mBackStack;
        if (arrayList != null) {
            return arrayList.size();
        }
        return 0;
    }

    public BackStackEntry getBackStackEntryAt(int index) {
        return (BackStackEntry) this.mBackStack.get(index);
    }

    public void addOnBackStackChangedListener(OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            this.mBackStackChangeListeners = new ArrayList<>();
        }
        this.mBackStackChangeListeners.add(listener);
    }

    public void removeOnBackStackChangedListener(OnBackStackChangedListener listener) {
        ArrayList<OnBackStackChangedListener> arrayList = this.mBackStackChangeListeners;
        if (arrayList != null) {
            arrayList.remove(listener);
        }
    }

    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fragment ");
            sb.append(fragment);
            sb.append(" is not currently in the FragmentManager");
            throwException(new IllegalStateException(sb.toString()));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        Fragment f = (Fragment) this.mActive.get(index);
        if (f == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fragment no longer exists for key ");
            sb.append(key);
            sb.append(": index ");
            sb.append(index);
            throwException(new IllegalStateException(sb.toString()));
        }
        return f;
    }

    public List<Fragment> getFragments() {
        List<Fragment> list;
        if (this.mAdded.isEmpty()) {
            return Collections.EMPTY_LIST;
        }
        synchronized (this.mAdded) {
            list = (List) this.mAdded.clone();
        }
        return list;
    }

    /* access modifiers changed from: 0000 */
    public List<Fragment> getActiveFragments() {
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray == null) {
            return null;
        }
        int count = sparseArray.size();
        ArrayList<Fragment> fragments = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            fragments.add(this.mActive.valueAt(i));
        }
        return fragments;
    }

    /* access modifiers changed from: 0000 */
    public int getActiveFragmentCount() {
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray == null) {
            return 0;
        }
        return sparseArray.size();
    }

    public SavedState saveFragmentInstanceState(Fragment fragment) {
        if (fragment.mIndex < 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Fragment ");
            sb.append(fragment);
            sb.append(" is not currently in the FragmentManager");
            throwException(new IllegalStateException(sb.toString()));
        }
        SavedState savedState = null;
        if (fragment.mState <= 0) {
            return null;
        }
        Bundle result = saveFragmentBasicState(fragment);
        if (result != null) {
            savedState = new SavedState(result);
        }
        return savedState;
    }

    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        Fragment fragment = this.mParent;
        if (fragment != null) {
            DebugUtils.buildShortClassTag(fragment, sb);
        } else {
            DebugUtils.buildShortClassTag(this.mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.append("    ");
        String innerPrefix = sb.toString();
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray != null) {
            int N = sparseArray.size();
            if (N > 0) {
                writer.print(prefix);
                writer.print("Active Fragments in ");
                writer.print(Integer.toHexString(System.identityHashCode(this)));
                writer.println(":");
                for (int i = 0; i < N; i++) {
                    Fragment f = (Fragment) this.mActive.valueAt(i);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i);
                    writer.print(": ");
                    writer.println(f);
                    if (f != null) {
                        f.dump(innerPrefix, fd, writer, args);
                    }
                }
            }
        }
        int N2 = this.mAdded.size();
        if (N2 > 0) {
            writer.print(prefix);
            writer.println("Added Fragments:");
            for (int i2 = 0; i2 < N2; i2++) {
                Fragment f2 = (Fragment) this.mAdded.get(i2);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i2);
                writer.print(": ");
                writer.println(f2.toString());
            }
        }
        ArrayList<Fragment> arrayList = this.mCreatedMenus;
        if (arrayList != null) {
            int N3 = arrayList.size();
            if (N3 > 0) {
                writer.print(prefix);
                writer.println("Fragments Created Menus:");
                for (int i3 = 0; i3 < N3; i3++) {
                    Fragment f3 = (Fragment) this.mCreatedMenus.get(i3);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i3);
                    writer.print(": ");
                    writer.println(f3.toString());
                }
            }
        }
        ArrayList<BackStackRecord> arrayList2 = this.mBackStack;
        if (arrayList2 != null) {
            int N4 = arrayList2.size();
            if (N4 > 0) {
                writer.print(prefix);
                writer.println("Back Stack:");
                for (int i4 = 0; i4 < N4; i4++) {
                    BackStackRecord bs = (BackStackRecord) this.mBackStack.get(i4);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i4);
                    writer.print(": ");
                    writer.println(bs.toString());
                    bs.dump(innerPrefix, fd, writer, args);
                }
            }
        }
        synchronized (this) {
            if (this.mBackStackIndices != null) {
                int N5 = this.mBackStackIndices.size();
                if (N5 > 0) {
                    writer.print(prefix);
                    writer.println("Back Stack Indices:");
                    for (int i5 = 0; i5 < N5; i5++) {
                        BackStackRecord bs2 = (BackStackRecord) this.mBackStackIndices.get(i5);
                        writer.print(prefix);
                        writer.print("  #");
                        writer.print(i5);
                        writer.print(": ");
                        writer.println(bs2);
                    }
                }
            }
            if (this.mAvailBackStackIndices != null && this.mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(this.mAvailBackStackIndices.toArray()));
            }
        }
        ArrayList<OpGenerator> arrayList3 = this.mPendingActions;
        if (arrayList3 != null) {
            int N6 = arrayList3.size();
            if (N6 > 0) {
                writer.print(prefix);
                writer.println("Pending Actions:");
                for (int i6 = 0; i6 < N6; i6++) {
                    OpGenerator r = (OpGenerator) this.mPendingActions.get(i6);
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i6);
                    writer.print(": ");
                    writer.println(r);
                }
            }
        }
        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(this.mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(this.mContainer);
        if (this.mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(this.mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(this.mCurState);
        writer.print(" mStateSaved=");
        writer.print(this.mStateSaved);
        writer.print(" mStopped=");
        writer.print(this.mStopped);
        writer.print(" mDestroyed=");
        writer.println(this.mDestroyed);
        if (this.mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(this.mNeedMenuInvalidate);
        }
        if (this.mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(this.mNoTransactionsBecause);
        }
    }

    static AnimationOrAnimator makeOpenCloseAnimation(Context context, float startScale, float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale, 1, 0.5f, 1, 0.5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(220);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(220);
        set.addAnimation(alpha);
        return new AnimationOrAnimator((Animation) set);
    }

    static AnimationOrAnimator makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(220);
        return new AnimationOrAnimator((Animation) anim);
    }

    /* access modifiers changed from: 0000 */
    public AnimationOrAnimator loadAnimation(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        int nextAnim = fragment.getNextAnim();
        Animation animation = fragment.onCreateAnimation(transit, enter, nextAnim);
        if (animation != null) {
            return new AnimationOrAnimator(animation);
        }
        Animator animator = fragment.onCreateAnimator(transit, enter, nextAnim);
        if (animator != null) {
            return new AnimationOrAnimator(animator);
        }
        if (nextAnim != 0) {
            boolean isAnim = "anim".equals(this.mHost.getContext().getResources().getResourceTypeName(nextAnim));
            boolean successfulLoad = false;
            if (isAnim) {
                try {
                    Animation animation2 = AnimationUtils.loadAnimation(this.mHost.getContext(), nextAnim);
                    if (animation2 != null) {
                        return new AnimationOrAnimator(animation2);
                    }
                    successfulLoad = true;
                } catch (NotFoundException e) {
                    throw e;
                } catch (RuntimeException e2) {
                }
            }
            if (!successfulLoad) {
                try {
                    Animator animator2 = AnimatorInflater.loadAnimator(this.mHost.getContext(), nextAnim);
                    if (animator2 != null) {
                        return new AnimationOrAnimator(animator2);
                    }
                } catch (RuntimeException e3) {
                    if (!isAnim) {
                        Animation animation3 = AnimationUtils.loadAnimation(this.mHost.getContext(), nextAnim);
                        if (animation3 != null) {
                            return new AnimationOrAnimator(animation3);
                        }
                    } else {
                        throw e3;
                    }
                }
            }
        }
        if (transit == 0) {
            return null;
        }
        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }
        switch (styleIndex) {
            case 1:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.125f, 1.0f, 0.0f, 1.0f);
            case 2:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 0.975f, 1.0f, 0.0f);
            case 3:
                return makeOpenCloseAnimation(this.mHost.getContext(), 0.975f, 1.0f, 0.0f, 1.0f);
            case 4:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 1.075f, 1.0f, 0.0f);
            case 5:
                return makeFadeAnimation(this.mHost.getContext(), 0.0f, 1.0f);
            case 6:
                return makeFadeAnimation(this.mHost.getContext(), 1.0f, 0.0f);
            default:
                if (transitionStyle == 0 && this.mHost.onHasWindowAnimations()) {
                    transitionStyle = this.mHost.onGetWindowAnimations();
                }
                return transitionStyle == 0 ? null : null;
        }
    }

    public void performPendingDeferredStart(Fragment f) {
        if (f.mDeferStart) {
            if (this.mExecutingActions) {
                this.mHavePendingDeferredStart = true;
                return;
            }
            f.mDeferStart = false;
            moveToState(f, this.mCurState, 0, 0, false);
        }
    }

    private static void setHWLayerAnimListenerIfAlpha(View v, AnimationOrAnimator anim) {
        if (!(v == null || anim == null || !shouldRunOnHWLayer(v, anim))) {
            if (anim.animator != null) {
                anim.animator.addListener(new AnimatorOnHWLayerIfNeededListener(v));
            } else {
                AnimationListener originalListener = getAnimationListener(anim.animation);
                v.setLayerType(2, null);
                anim.animation.setAnimationListener(new AnimateOnHWLayerIfNeededListener(v, originalListener));
            }
        }
    }

    private static AnimationListener getAnimationListener(Animation animation) {
        String str = TAG;
        try {
            if (sAnimationListenerField == null) {
                sAnimationListenerField = Animation.class.getDeclaredField("mListener");
                sAnimationListenerField.setAccessible(true);
            }
            return (AnimationListener) sAnimationListenerField.get(animation);
        } catch (NoSuchFieldException e) {
            Log.e(str, "No field with the name mListener is found in Animation class", e);
            return null;
        } catch (IllegalAccessException e2) {
            Log.e(str, "Cannot access Animation's mListener field", e2);
            return null;
        }
    }

    /* access modifiers changed from: 0000 */
    public boolean isStateAtLeast(int state) {
        return this.mCurState >= state;
    }

    /* access modifiers changed from: 0000 */
    /* JADX WARNING: Code restructure failed: missing block: B:41:0x0075, code lost:
        if (r1 != 4) goto L_0x02ff;
     */
    /* JADX WARNING: Removed duplicated region for block: B:132:0x02b8  */
    /* JADX WARNING: Removed duplicated region for block: B:134:0x02bc  */
    /* JADX WARNING: Removed duplicated region for block: B:140:0x02dd  */
    /* JADX WARNING: Removed duplicated region for block: B:91:0x01be  */
    public void moveToState(Fragment f, int newState, int transit, int transitionStyle, boolean keepActive) {
        int newState2;
        int newState3;
        String resName;
        FragmentManagerImpl fragmentManagerImpl;
        String str;
        Fragment fragment = f;
        boolean z = true;
        if (!fragment.mAdded || fragment.mDetached) {
            newState2 = newState;
            if (newState2 > 1) {
                newState2 = 1;
            }
        } else {
            newState2 = newState;
        }
        if (fragment.mRemoving && newState2 > fragment.mState) {
            if (fragment.mState != 0 || !f.isInBackStack()) {
                newState2 = fragment.mState;
            } else {
                newState2 = 1;
            }
        }
        if (fragment.mDeferStart && fragment.mState < 4 && newState2 > 3) {
            newState2 = 3;
        }
        int i = fragment.mState;
        String str2 = TAG;
        if (i <= newState2) {
            if (!fragment.mFromLayout || fragment.mInLayout) {
                if (!(f.getAnimatingAway() == null && f.getAnimator() == null)) {
                    fragment.setAnimatingAway(null);
                    fragment.setAnimator(null);
                    moveToState(f, f.getStateAfterAnimating(), 0, 0, true);
                }
                int i2 = fragment.mState;
                if (i2 != 0) {
                    if (i2 != 1) {
                        if (i2 != 2) {
                            if (i2 != 3) {
                            }
                            if (newState2 > 3) {
                                if (DEBUG) {
                                    StringBuilder sb = new StringBuilder();
                                    sb.append("moveto STARTED: ");
                                    sb.append(fragment);
                                    Log.v(str2, sb.toString());
                                }
                                f.performStart();
                                dispatchOnFragmentStarted(fragment, false);
                            }
                            if (newState2 > 4) {
                                if (DEBUG) {
                                    StringBuilder sb2 = new StringBuilder();
                                    sb2.append("moveto RESUMED: ");
                                    sb2.append(fragment);
                                    Log.v(str2, sb2.toString());
                                }
                                f.performResume();
                                dispatchOnFragmentResumed(fragment, false);
                                fragment.mSavedFragmentState = null;
                                fragment.mSavedViewState = null;
                            }
                            int i3 = transit;
                            int i4 = transitionStyle;
                        }
                        if (newState2 > 2) {
                            fragment.mState = 3;
                        }
                        if (newState2 > 3) {
                        }
                        if (newState2 > 4) {
                        }
                        int i32 = transit;
                        int i42 = transitionStyle;
                    }
                } else if (newState2 > 0) {
                    if (DEBUG) {
                        StringBuilder sb3 = new StringBuilder();
                        sb3.append("moveto CREATED: ");
                        sb3.append(fragment);
                        Log.v(str2, sb3.toString());
                    }
                    if (fragment.mSavedFragmentState != null) {
                        fragment.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                        fragment.mSavedViewState = fragment.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                        fragment.mTarget = getFragment(fragment.mSavedFragmentState, TARGET_STATE_TAG);
                        if (fragment.mTarget != null) {
                            fragment.mTargetRequestCode = fragment.mSavedFragmentState.getInt(TARGET_REQUEST_CODE_STATE_TAG, 0);
                        }
                        if (fragment.mSavedUserVisibleHint != null) {
                            fragment.mUserVisibleHint = fragment.mSavedUserVisibleHint.booleanValue();
                            fragment.mSavedUserVisibleHint = null;
                        } else {
                            fragment.mUserVisibleHint = fragment.mSavedFragmentState.getBoolean(USER_VISIBLE_HINT_TAG, true);
                        }
                        if (!fragment.mUserVisibleHint) {
                            fragment.mDeferStart = true;
                            if (newState2 > 3) {
                                newState2 = 3;
                            }
                        }
                    }
                    FragmentHostCallback fragmentHostCallback = this.mHost;
                    fragment.mHost = fragmentHostCallback;
                    Fragment fragment2 = this.mParent;
                    fragment.mParentFragment = fragment2;
                    if (fragment2 != null) {
                        fragmentManagerImpl = fragment2.mChildFragmentManager;
                    } else {
                        fragmentManagerImpl = fragmentHostCallback.getFragmentManagerImpl();
                    }
                    fragment.mFragmentManager = fragmentManagerImpl;
                    String str3 = "Fragment ";
                    if (fragment.mTarget == null) {
                        str = str3;
                    } else if (this.mActive.get(fragment.mTarget.mIndex) != fragment.mTarget) {
                        String str4 = str3;
                        StringBuilder sb4 = new StringBuilder();
                        sb4.append(str4);
                        sb4.append(fragment);
                        sb4.append(" declared target fragment ");
                        sb4.append(fragment.mTarget);
                        sb4.append(" that does not belong to this FragmentManager!");
                        throw new IllegalStateException(sb4.toString());
                    } else if (fragment.mTarget.mState < 1) {
                        str = str3;
                        moveToState(fragment.mTarget, 1, 0, 0, true);
                    } else {
                        str = str3;
                    }
                    dispatchOnFragmentPreAttached(fragment, this.mHost.getContext(), false);
                    fragment.mCalled = false;
                    fragment.onAttach(this.mHost.getContext());
                    if (fragment.mCalled) {
                        if (fragment.mParentFragment == null) {
                            this.mHost.onAttachFragment(fragment);
                        } else {
                            fragment.mParentFragment.onAttachFragment(fragment);
                        }
                        dispatchOnFragmentAttached(fragment, this.mHost.getContext(), false);
                        if (!fragment.mIsCreated) {
                            dispatchOnFragmentPreCreated(fragment, fragment.mSavedFragmentState, false);
                            fragment.performCreate(fragment.mSavedFragmentState);
                            dispatchOnFragmentCreated(fragment, fragment.mSavedFragmentState, false);
                        } else {
                            fragment.restoreChildFragmentState(fragment.mSavedFragmentState);
                            fragment.mState = 1;
                        }
                        fragment.mRetaining = false;
                        newState3 = newState2;
                        ensureInflatedFragmentView(f);
                        if (newState3 > 1) {
                            if (DEBUG) {
                                StringBuilder sb5 = new StringBuilder();
                                sb5.append("moveto ACTIVITY_CREATED: ");
                                sb5.append(fragment);
                                Log.v(str2, sb5.toString());
                            }
                            if (!fragment.mFromLayout) {
                                ViewGroup container = null;
                                if (fragment.mContainerId != 0) {
                                    if (fragment.mContainerId == -1) {
                                        StringBuilder sb6 = new StringBuilder();
                                        sb6.append("Cannot create fragment ");
                                        sb6.append(fragment);
                                        sb6.append(" for a container view with no id");
                                        throwException(new IllegalArgumentException(sb6.toString()));
                                    }
                                    ViewGroup container2 = (ViewGroup) this.mContainer.onFindViewById(fragment.mContainerId);
                                    if (container2 == null && !fragment.mRestored) {
                                        try {
                                            resName = f.getResources().getResourceName(fragment.mContainerId);
                                        } catch (NotFoundException e) {
                                            resName = EnvironmentCompat.MEDIA_UNKNOWN;
                                        }
                                        StringBuilder sb7 = new StringBuilder();
                                        sb7.append("No view found for id 0x");
                                        sb7.append(Integer.toHexString(fragment.mContainerId));
                                        sb7.append(" (");
                                        sb7.append(resName);
                                        sb7.append(") for fragment ");
                                        sb7.append(fragment);
                                        throwException(new IllegalArgumentException(sb7.toString()));
                                    }
                                    container = container2;
                                }
                                fragment.mContainer = container;
                                fragment.mView = fragment.performCreateView(fragment.performGetLayoutInflater(fragment.mSavedFragmentState), container, fragment.mSavedFragmentState);
                                if (fragment.mView != null) {
                                    fragment.mInnerView = fragment.mView;
                                    fragment.mView.setSaveFromParentEnabled(false);
                                    if (container != null) {
                                        container.addView(fragment.mView);
                                    }
                                    if (fragment.mHidden) {
                                        fragment.mView.setVisibility(8);
                                    }
                                    fragment.onViewCreated(fragment.mView, fragment.mSavedFragmentState);
                                    dispatchOnFragmentViewCreated(fragment, fragment.mView, fragment.mSavedFragmentState, false);
                                    if (fragment.mView.getVisibility() != 0 || fragment.mContainer == null) {
                                        z = false;
                                    }
                                    fragment.mIsNewlyAdded = z;
                                } else {
                                    fragment.mInnerView = null;
                                }
                            }
                            fragment.performActivityCreated(fragment.mSavedFragmentState);
                            dispatchOnFragmentActivityCreated(fragment, fragment.mSavedFragmentState, false);
                            if (fragment.mView != null) {
                                fragment.restoreViewState(fragment.mSavedFragmentState);
                            }
                            fragment.mSavedFragmentState = null;
                        }
                        newState2 = newState3;
                        if (newState2 > 2) {
                        }
                        if (newState2 > 3) {
                        }
                        if (newState2 > 4) {
                        }
                        int i322 = transit;
                        int i422 = transitionStyle;
                    } else {
                        StringBuilder sb8 = new StringBuilder();
                        sb8.append(str);
                        sb8.append(fragment);
                        sb8.append(" did not call through to super.onAttach()");
                        throw new SuperNotCalledException(sb8.toString());
                    }
                }
                newState3 = newState2;
                ensureInflatedFragmentView(f);
                if (newState3 > 1) {
                }
                newState2 = newState3;
                if (newState2 > 2) {
                }
                if (newState2 > 3) {
                }
                if (newState2 > 4) {
                }
                int i3222 = transit;
                int i4222 = transitionStyle;
            } else {
                return;
            }
        } else if (fragment.mState > newState2) {
            int i5 = fragment.mState;
            if (i5 != 1) {
                if (i5 != 2) {
                    if (i5 != 3) {
                        if (i5 != 4) {
                            if (i5 != 5) {
                                int i6 = transit;
                                int i7 = transitionStyle;
                            } else if (newState2 < 5) {
                                if (DEBUG) {
                                    StringBuilder sb9 = new StringBuilder();
                                    sb9.append("movefrom RESUMED: ");
                                    sb9.append(fragment);
                                    Log.v(str2, sb9.toString());
                                }
                                f.performPause();
                                dispatchOnFragmentPaused(fragment, false);
                            }
                        }
                        if (newState2 < 4) {
                            if (DEBUG) {
                                StringBuilder sb10 = new StringBuilder();
                                sb10.append("movefrom STARTED: ");
                                sb10.append(fragment);
                                Log.v(str2, sb10.toString());
                            }
                            f.performStop();
                            dispatchOnFragmentStopped(fragment, false);
                        }
                    }
                    if (newState2 < 3) {
                        if (DEBUG) {
                            StringBuilder sb11 = new StringBuilder();
                            sb11.append("movefrom STOPPED: ");
                            sb11.append(fragment);
                            Log.v(str2, sb11.toString());
                        }
                        f.performReallyStop();
                    }
                }
                if (newState2 < 2) {
                    if (DEBUG) {
                        StringBuilder sb12 = new StringBuilder();
                        sb12.append("movefrom ACTIVITY_CREATED: ");
                        sb12.append(fragment);
                        Log.v(str2, sb12.toString());
                    }
                    if (fragment.mView != null && this.mHost.onShouldSaveFragmentState(fragment) && fragment.mSavedViewState == null) {
                        saveFragmentViewState(f);
                    }
                    f.performDestroyView();
                    dispatchOnFragmentViewDestroyed(fragment, false);
                    if (fragment.mView == null || fragment.mContainer == null) {
                        int i8 = transit;
                        int i9 = transitionStyle;
                    } else {
                        fragment.mContainer.endViewTransition(fragment.mView);
                        fragment.mView.clearAnimation();
                        AnimationOrAnimator anim = null;
                        if (this.mCurState <= 0 || this.mDestroyed) {
                            int i10 = transit;
                            int i11 = transitionStyle;
                        } else if (fragment.mView.getVisibility() != 0 || fragment.mPostponedAlpha < 0.0f) {
                            int i12 = transit;
                            int i13 = transitionStyle;
                        } else {
                            anim = loadAnimation(fragment, transit, false, transitionStyle);
                        }
                        fragment.mPostponedAlpha = 0.0f;
                        if (anim != null) {
                            animateRemoveFragment(fragment, anim, newState2);
                        }
                        fragment.mContainer.removeView(fragment.mView);
                    }
                    fragment.mContainer = null;
                    fragment.mView = null;
                    fragment.mInnerView = null;
                    fragment.mInLayout = false;
                } else {
                    int i14 = transit;
                    int i15 = transitionStyle;
                }
            } else {
                int i16 = transit;
                int i17 = transitionStyle;
            }
            if (newState2 < 1) {
                if (this.mDestroyed) {
                    if (f.getAnimatingAway() != null) {
                        View v = f.getAnimatingAway();
                        fragment.setAnimatingAway(null);
                        v.clearAnimation();
                    } else if (f.getAnimator() != null) {
                        Animator animator = f.getAnimator();
                        fragment.setAnimator(null);
                        animator.cancel();
                    }
                }
                if (f.getAnimatingAway() == null && f.getAnimator() == null) {
                    if (DEBUG) {
                        StringBuilder sb13 = new StringBuilder();
                        sb13.append("movefrom CREATED: ");
                        sb13.append(fragment);
                        Log.v(str2, sb13.toString());
                    }
                    if (!fragment.mRetaining) {
                        f.performDestroy();
                        dispatchOnFragmentDestroyed(fragment, false);
                    } else {
                        fragment.mState = 0;
                    }
                    f.performDetach();
                    dispatchOnFragmentDetached(fragment, false);
                    if (!keepActive) {
                        if (!fragment.mRetaining) {
                            makeInactive(f);
                        } else {
                            fragment.mHost = null;
                            fragment.mParentFragment = null;
                            fragment.mFragmentManager = null;
                        }
                    }
                } else {
                    fragment.setStateAfterAnimating(newState2);
                    newState2 = 1;
                }
            }
        } else {
            int i18 = transit;
            int i19 = transitionStyle;
        }
        if (fragment.mState != newState2) {
            StringBuilder sb14 = new StringBuilder();
            sb14.append("moveToState: Fragment state for ");
            sb14.append(fragment);
            sb14.append(" not updated inline; ");
            sb14.append("expected state ");
            sb14.append(newState2);
            sb14.append(" found ");
            sb14.append(fragment.mState);
            Log.w(str2, sb14.toString());
            fragment.mState = newState2;
        }
    }

    private void animateRemoveFragment(final Fragment fragment, AnimationOrAnimator anim, int newState) {
        final View viewToAnimate = fragment.mView;
        final ViewGroup container = fragment.mContainer;
        container.startViewTransition(viewToAnimate);
        fragment.setStateAfterAnimating(newState);
        if (anim.animation != null) {
            Animation animation = new EndViewTransitionAnimator(anim.animation, container, viewToAnimate);
            fragment.setAnimatingAway(fragment.mView);
            animation.setAnimationListener(new AnimationListenerWrapper(getAnimationListener(animation)) {
                public void onAnimationEnd(Animation animation) {
                    super.onAnimationEnd(animation);
                    container.post(new Runnable() {
                        public void run() {
                            if (fragment.getAnimatingAway() != null) {
                                fragment.setAnimatingAway(null);
                                FragmentManagerImpl.this.moveToState(fragment, fragment.getStateAfterAnimating(), 0, 0, false);
                            }
                        }
                    });
                }
            });
            setHWLayerAnimListenerIfAlpha(viewToAnimate, anim);
            fragment.mView.startAnimation(animation);
            return;
        }
        Animator animator = anim.animator;
        fragment.setAnimator(anim.animator);
        animator.addListener(new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator anim) {
                container.endViewTransition(viewToAnimate);
                Animator animator = fragment.getAnimator();
                fragment.setAnimator(null);
                if (animator != null && container.indexOfChild(viewToAnimate) < 0) {
                    FragmentManagerImpl fragmentManagerImpl = FragmentManagerImpl.this;
                    Fragment fragment = fragment;
                    fragmentManagerImpl.moveToState(fragment, fragment.getStateAfterAnimating(), 0, 0, false);
                }
            }
        });
        animator.setTarget(fragment.mView);
        setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
        animator.start();
    }

    /* access modifiers changed from: 0000 */
    public void moveToState(Fragment f) {
        moveToState(f, this.mCurState, 0, 0, false);
    }

    /* access modifiers changed from: 0000 */
    public void ensureInflatedFragmentView(Fragment f) {
        if (f.mFromLayout && !f.mPerformedCreateView) {
            f.mView = f.performCreateView(f.performGetLayoutInflater(f.mSavedFragmentState), null, f.mSavedFragmentState);
            if (f.mView != null) {
                f.mInnerView = f.mView;
                f.mView.setSaveFromParentEnabled(false);
                if (f.mHidden) {
                    f.mView.setVisibility(8);
                }
                f.onViewCreated(f.mView, f.mSavedFragmentState);
                dispatchOnFragmentViewCreated(f, f.mView, f.mSavedFragmentState, false);
                return;
            }
            f.mInnerView = null;
        }
    }

    /* access modifiers changed from: 0000 */
    public void completeShowHideFragment(final Fragment fragment) {
        if (fragment.mView != null) {
            AnimationOrAnimator anim = loadAnimation(fragment, fragment.getNextTransition(), !fragment.mHidden, fragment.getNextTransitionStyle());
            if (anim == null || anim.animator == null) {
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                    fragment.mView.startAnimation(anim.animation);
                    anim.animation.start();
                }
                fragment.mView.setVisibility((!fragment.mHidden || fragment.isHideReplaced()) ? 0 : 8);
                if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                }
            } else {
                anim.animator.setTarget(fragment.mView);
                if (!fragment.mHidden) {
                    fragment.mView.setVisibility(0);
                } else if (fragment.isHideReplaced()) {
                    fragment.setHideReplaced(false);
                } else {
                    final ViewGroup container = fragment.mContainer;
                    final View animatingView = fragment.mView;
                    container.startViewTransition(animatingView);
                    anim.animator.addListener(new AnimatorListenerAdapter() {
                        public void onAnimationEnd(Animator animation) {
                            container.endViewTransition(animatingView);
                            animation.removeListener(this);
                            if (fragment.mView != null) {
                                fragment.mView.setVisibility(8);
                            }
                        }
                    });
                }
                setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                anim.animator.start();
            }
        }
        if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        fragment.mHiddenChanged = false;
        fragment.onHiddenChanged(fragment.mHidden);
    }

    /* access modifiers changed from: 0000 */
    public void moveFragmentToExpectedState(Fragment f) {
        if (f != null) {
            int nextState = this.mCurState;
            if (f.mRemoving) {
                if (f.isInBackStack()) {
                    nextState = Math.min(nextState, 1);
                } else {
                    nextState = Math.min(nextState, 0);
                }
            }
            moveToState(f, nextState, f.getNextTransition(), f.getNextTransitionStyle(), false);
            if (f.mView != null) {
                Fragment underFragment = findFragmentUnder(f);
                if (underFragment != null) {
                    View underView = underFragment.mView;
                    ViewGroup container = f.mContainer;
                    int underIndex = container.indexOfChild(underView);
                    int viewIndex = container.indexOfChild(f.mView);
                    if (viewIndex < underIndex) {
                        container.removeViewAt(viewIndex);
                        container.addView(f.mView, underIndex);
                    }
                }
                if (f.mIsNewlyAdded && f.mContainer != null) {
                    if (f.mPostponedAlpha > 0.0f) {
                        f.mView.setAlpha(f.mPostponedAlpha);
                    }
                    f.mPostponedAlpha = 0.0f;
                    f.mIsNewlyAdded = false;
                    AnimationOrAnimator anim = loadAnimation(f, f.getNextTransition(), true, f.getNextTransitionStyle());
                    if (anim != null) {
                        setHWLayerAnimListenerIfAlpha(f.mView, anim);
                        if (anim.animation != null) {
                            f.mView.startAnimation(anim.animation);
                        } else {
                            anim.animator.setTarget(f.mView);
                            anim.animator.start();
                        }
                    }
                }
            }
            if (f.mHiddenChanged) {
                completeShowHideFragment(f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void moveToState(int newState, boolean always) {
        if (this.mHost == null && newState != 0) {
            throw new IllegalStateException("No activity");
        } else if (always || newState != this.mCurState) {
            this.mCurState = newState;
            if (this.mActive != null) {
                int numAdded = this.mAdded.size();
                for (int i = 0; i < numAdded; i++) {
                    moveFragmentToExpectedState((Fragment) this.mAdded.get(i));
                }
                int numActive = this.mActive.size();
                for (int i2 = 0; i2 < numActive; i2++) {
                    Fragment f = (Fragment) this.mActive.valueAt(i2);
                    if (f != null && ((f.mRemoving || f.mDetached) && !f.mIsNewlyAdded)) {
                        moveFragmentToExpectedState(f);
                    }
                }
                startPendingDeferredFragments();
                if (this.mNeedMenuInvalidate) {
                    FragmentHostCallback fragmentHostCallback = this.mHost;
                    if (fragmentHostCallback != null && this.mCurState == 5) {
                        fragmentHostCallback.onSupportInvalidateOptionsMenu();
                        this.mNeedMenuInvalidate = false;
                    }
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void startPendingDeferredFragments() {
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    performPendingDeferredStart(f);
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void makeActive(Fragment f) {
        if (f.mIndex < 0) {
            int i = this.mNextFragmentIndex;
            this.mNextFragmentIndex = i + 1;
            f.setIndex(i, this.mParent);
            if (this.mActive == null) {
                this.mActive = new SparseArray<>();
            }
            this.mActive.put(f.mIndex, f);
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Allocated fragment index ");
                sb.append(f);
                Log.v(TAG, sb.toString());
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void makeInactive(Fragment f) {
        if (f.mIndex >= 0) {
            if (DEBUG) {
                StringBuilder sb = new StringBuilder();
                sb.append("Freeing fragment index ");
                sb.append(f);
                Log.v(TAG, sb.toString());
            }
            this.mActive.put(f.mIndex, null);
            f.initState();
        }
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("add: ");
            sb.append(fragment);
            Log.v(TAG, sb.toString());
        }
        makeActive(fragment);
        if (fragment.mDetached) {
            return;
        }
        if (!this.mAdded.contains(fragment)) {
            synchronized (this.mAdded) {
                this.mAdded.add(fragment);
            }
            fragment.mAdded = true;
            fragment.mRemoving = false;
            if (fragment.mView == null) {
                fragment.mHiddenChanged = false;
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            if (moveToStateNow) {
                moveToState(fragment);
                return;
            }
            return;
        }
        StringBuilder sb2 = new StringBuilder();
        sb2.append("Fragment already added: ");
        sb2.append(fragment);
        throw new IllegalStateException(sb2.toString());
    }

    public void removeFragment(Fragment fragment) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("remove: ");
            sb.append(fragment);
            sb.append(" nesting=");
            sb.append(fragment.mBackStackNesting);
            Log.v(TAG, sb.toString());
        }
        boolean inactive = !fragment.isInBackStack();
        if (!fragment.mDetached || inactive) {
            synchronized (this.mAdded) {
                this.mAdded.remove(fragment);
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
        }
    }

    public void hideFragment(Fragment fragment) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("hide: ");
            sb.append(fragment);
            Log.v(TAG, sb.toString());
        }
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            fragment.mHiddenChanged = true ^ fragment.mHiddenChanged;
        }
    }

    public void showFragment(Fragment fragment) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("show: ");
            sb.append(fragment);
            Log.v(TAG, sb.toString());
        }
        if (fragment.mHidden) {
            fragment.mHidden = false;
            fragment.mHiddenChanged = !fragment.mHiddenChanged;
        }
    }

    public void detachFragment(Fragment fragment) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("detach: ");
            sb.append(fragment);
            Log.v(TAG, sb.toString());
        }
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                if (DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("remove from detach: ");
                    sb2.append(fragment);
                    Log.v(TAG, sb2.toString());
                }
                synchronized (this.mAdded) {
                    this.mAdded.remove(fragment);
                }
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
            }
        }
    }

    public void attachFragment(Fragment fragment) {
        if (DEBUG) {
            StringBuilder sb = new StringBuilder();
            sb.append("attach: ");
            sb.append(fragment);
            Log.v(TAG, sb.toString());
        }
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (fragment.mAdded) {
                return;
            }
            if (!this.mAdded.contains(fragment)) {
                if (DEBUG) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("add from attach: ");
                    sb2.append(fragment);
                    Log.v(TAG, sb2.toString());
                }
                synchronized (this.mAdded) {
                    this.mAdded.add(fragment);
                }
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                    return;
                }
                return;
            }
            StringBuilder sb3 = new StringBuilder();
            sb3.append("Fragment already added: ");
            sb3.append(fragment);
            throw new IllegalStateException(sb3.toString());
        }
    }

    public Fragment findFragmentById(int id) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.mFragmentId == id) {
                return f;
            }
        }
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray != null) {
            for (int i2 = sparseArray.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = (Fragment) this.mActive.valueAt(i2);
                if (f2 != null && f2.mFragmentId == id) {
                    return f2;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByTag(String tag) {
        if (tag != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        SparseArray<Fragment> sparseArray = this.mActive;
        if (!(sparseArray == null || tag == null)) {
            for (int i2 = sparseArray.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = (Fragment) this.mActive.valueAt(i2);
                if (f2 != null && tag.equals(f2.mTag)) {
                    return f2;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        SparseArray<Fragment> sparseArray = this.mActive;
        if (!(sparseArray == null || who == null)) {
            for (int i = sparseArray.size() - 1; i >= 0; i--) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    Fragment findFragmentByWho = f.findFragmentByWho(who);
                    Fragment f2 = findFragmentByWho;
                    if (findFragmentByWho != null) {
                        return f2;
                    }
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (isStateSaved()) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        } else if (this.mNoTransactionsBecause != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Can not perform this action inside of ");
            sb.append(this.mNoTransactionsBecause);
            throw new IllegalStateException(sb.toString());
        }
    }

    public boolean isStateSaved() {
        return this.mStateSaved || this.mStopped;
    }

    public void enqueueAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (!this.mDestroyed) {
                if (this.mHost != null) {
                    if (this.mPendingActions == null) {
                        this.mPendingActions = new ArrayList<>();
                    }
                    this.mPendingActions.add(action);
                    scheduleCommit();
                    return;
                }
            }
            if (!allowStateLoss) {
                throw new IllegalStateException("Activity has been destroyed");
            }
        }
    }

    /* access modifiers changed from: private */
    public void scheduleCommit() {
        synchronized (this) {
            boolean pendingReady = false;
            boolean postponeReady = this.mPostponedTransactions != null && !this.mPostponedTransactions.isEmpty();
            if (this.mPendingActions != null && this.mPendingActions.size() == 1) {
                pendingReady = true;
            }
            if (postponeReady || pendingReady) {
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                this.mHost.getHandler().post(this.mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (this.mAvailBackStackIndices != null) {
                if (this.mAvailBackStackIndices.size() > 0) {
                    int index = ((Integer) this.mAvailBackStackIndices.remove(this.mAvailBackStackIndices.size() - 1)).intValue();
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder sb = new StringBuilder();
                        sb.append("Adding back stack index ");
                        sb.append(index);
                        sb.append(" with ");
                        sb.append(bse);
                        Log.v(str, sb.toString());
                    }
                    this.mBackStackIndices.set(index, bse);
                    return index;
                }
            }
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList<>();
            }
            int index2 = this.mBackStackIndices.size();
            if (DEBUG) {
                String str2 = TAG;
                StringBuilder sb2 = new StringBuilder();
                sb2.append("Setting back stack index ");
                sb2.append(index2);
                sb2.append(" to ");
                sb2.append(bse);
                Log.v(str2, sb2.toString());
            }
            this.mBackStackIndices.add(bse);
            return index2;
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList<>();
            }
            int N = this.mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) {
                    String str = TAG;
                    StringBuilder sb = new StringBuilder();
                    sb.append("Setting back stack index ");
                    sb.append(index);
                    sb.append(" to ");
                    sb.append(bse);
                    Log.v(str, sb.toString());
                }
                this.mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    this.mBackStackIndices.add(null);
                    if (this.mAvailBackStackIndices == null) {
                        this.mAvailBackStackIndices = new ArrayList<>();
                    }
                    if (DEBUG) {
                        String str2 = TAG;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("Adding available back stack index ");
                        sb2.append(N);
                        Log.v(str2, sb2.toString());
                    }
                    this.mAvailBackStackIndices.add(Integer.valueOf(N));
                    N++;
                }
                if (DEBUG) {
                    String str3 = TAG;
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Adding back stack index ");
                    sb3.append(index);
                    sb3.append(" with ");
                    sb3.append(bse);
                    Log.v(str3, sb3.toString());
                }
                this.mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            this.mBackStackIndices.set(index, null);
            if (this.mAvailBackStackIndices == null) {
                this.mAvailBackStackIndices = new ArrayList<>();
            }
            if (DEBUG) {
                String str = TAG;
                StringBuilder sb = new StringBuilder();
                sb.append("Freeing back stack index ");
                sb.append(index);
                Log.v(str, sb.toString());
            }
            this.mAvailBackStackIndices.add(Integer.valueOf(index));
        }
    }

    private void ensureExecReady(boolean allowStateLoss) {
        if (this.mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        } else if (this.mHost == null) {
            throw new IllegalStateException("Fragment host has been destroyed");
        } else if (Looper.myLooper() == this.mHost.getHandler().getLooper()) {
            if (!allowStateLoss) {
                checkStateLoss();
            }
            if (this.mTmpRecords == null) {
                this.mTmpRecords = new ArrayList<>();
                this.mTmpIsPop = new ArrayList<>();
            }
            this.mExecutingActions = true;
            try {
                executePostponedTransaction(null, null);
            } finally {
                this.mExecutingActions = false;
            }
        } else {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        }
    }

    public void execSingleAction(OpGenerator action, boolean allowStateLoss) {
        if (!allowStateLoss || (this.mHost != null && !this.mDestroyed)) {
            ensureExecReady(allowStateLoss);
            if (action.generateOps(this.mTmpRecords, this.mTmpIsPop)) {
                this.mExecutingActions = true;
                try {
                    removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
                } finally {
                    cleanupExec();
                }
            }
            doPendingDeferredStart();
            burpActive();
        }
    }

    private void cleanupExec() {
        this.mExecutingActions = false;
        this.mTmpIsPop.clear();
        this.mTmpRecords.clear();
    }

    /* JADX INFO: finally extract failed */
    public boolean execPendingActions() {
        ensureExecReady(true);
        boolean didSomething = false;
        while (generateOpsForPendingActions(this.mTmpRecords, this.mTmpIsPop)) {
            this.mExecutingActions = true;
            try {
                removeRedundantOperationsAndExecute(this.mTmpRecords, this.mTmpIsPop);
                cleanupExec();
                didSomething = true;
            } catch (Throwable th) {
                cleanupExec();
                throw th;
            }
        }
        doPendingDeferredStart();
        burpActive();
        return didSomething;
    }

    private void executePostponedTransaction(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
        ArrayList<StartEnterTransitionListener> arrayList = this.mPostponedTransactions;
        int numPostponed = arrayList == null ? 0 : arrayList.size();
        int i = 0;
        while (i < numPostponed) {
            StartEnterTransitionListener listener = (StartEnterTransitionListener) this.mPostponedTransactions.get(i);
            if (records != null && !listener.mIsBack) {
                int index = records.indexOf(listener.mRecord);
                if (index != -1 && ((Boolean) isRecordPop.get(index)).booleanValue()) {
                    listener.cancelTransaction();
                    i++;
                }
            }
            if (listener.isReady() != 0 || (records != null && listener.mRecord.interactsWith(records, 0, records.size()))) {
                this.mPostponedTransactions.remove(i);
                i--;
                numPostponed--;
                if (records != null && !listener.mIsBack) {
                    int indexOf = records.indexOf(listener.mRecord);
                    int index2 = indexOf;
                    if (indexOf != -1 && ((Boolean) isRecordPop.get(index2)).booleanValue()) {
                        listener.cancelTransaction();
                    }
                }
                listener.completeTransaction();
            }
            i++;
        }
    }

    private void removeRedundantOperationsAndExecute(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop) {
        if (records != null && !records.isEmpty()) {
            if (isRecordPop == null || records.size() != isRecordPop.size()) {
                throw new IllegalStateException("Internal error with the back stack records");
            }
            executePostponedTransaction(records, isRecordPop);
            int numRecords = records.size();
            int startIndex = 0;
            int recordNum = 0;
            while (recordNum < numRecords) {
                if (!((BackStackRecord) records.get(recordNum)).mReorderingAllowed) {
                    if (startIndex != recordNum) {
                        executeOpsTogether(records, isRecordPop, startIndex, recordNum);
                    }
                    int reorderingEnd = recordNum + 1;
                    if (((Boolean) isRecordPop.get(recordNum)).booleanValue()) {
                        while (reorderingEnd < numRecords && ((Boolean) isRecordPop.get(reorderingEnd)).booleanValue() && !((BackStackRecord) records.get(reorderingEnd)).mReorderingAllowed) {
                            reorderingEnd++;
                        }
                    }
                    executeOpsTogether(records, isRecordPop, recordNum, reorderingEnd);
                    startIndex = reorderingEnd;
                    recordNum = reorderingEnd - 1;
                }
                recordNum++;
            }
            if (startIndex != numRecords) {
                executeOpsTogether(records, isRecordPop, startIndex, numRecords);
            }
        }
    }

    private void executeOpsTogether(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        ArrayList<BackStackRecord> arrayList = records;
        ArrayList<Boolean> arrayList2 = isRecordPop;
        int i = startIndex;
        int i2 = endIndex;
        boolean allowReordering = ((BackStackRecord) arrayList.get(i)).mReorderingAllowed;
        ArrayList<Fragment> arrayList3 = this.mTmpAddedFragments;
        if (arrayList3 == null) {
            this.mTmpAddedFragments = new ArrayList<>();
        } else {
            arrayList3.clear();
        }
        this.mTmpAddedFragments.addAll(this.mAdded);
        int recordNum = startIndex;
        boolean addToBackStack = false;
        Fragment oldPrimaryNav = getPrimaryNavigationFragment();
        while (true) {
            boolean z = true;
            if (recordNum >= i2) {
                break;
            }
            BackStackRecord record = (BackStackRecord) arrayList.get(recordNum);
            if (!((Boolean) arrayList2.get(recordNum)).booleanValue()) {
                oldPrimaryNav = record.expandOps(this.mTmpAddedFragments, oldPrimaryNav);
            } else {
                oldPrimaryNav = record.trackAddedFragmentsInPop(this.mTmpAddedFragments, oldPrimaryNav);
            }
            if (!addToBackStack && !record.mAddToBackStack) {
                z = false;
            }
            addToBackStack = z;
            recordNum++;
        }
        this.mTmpAddedFragments.clear();
        if (!allowReordering) {
            FragmentTransition.startTransitions(this, records, isRecordPop, startIndex, endIndex, false);
        }
        executeOps(records, isRecordPop, startIndex, endIndex);
        int postponeIndex = endIndex;
        if (allowReordering) {
            ArraySet arraySet = new ArraySet();
            addAddedFragments(arraySet);
            ArraySet arraySet2 = arraySet;
            int postponeIndex2 = postponePostponableTransactions(records, isRecordPop, startIndex, endIndex, arraySet);
            makeRemovedFragmentsInvisible(arraySet2);
            postponeIndex = postponeIndex2;
        }
        if (postponeIndex != i && allowReordering) {
            FragmentTransition.startTransitions(this, records, isRecordPop, startIndex, postponeIndex, true);
            moveToState(this.mCurState, true);
        }
        for (int recordNum2 = startIndex; recordNum2 < i2; recordNum2++) {
            BackStackRecord record2 = (BackStackRecord) arrayList.get(recordNum2);
            if (((Boolean) arrayList2.get(recordNum2)).booleanValue() && record2.mIndex >= 0) {
                freeBackStackIndex(record2.mIndex);
                record2.mIndex = -1;
            }
            record2.runOnCommitRunnables();
        }
        if (addToBackStack) {
            reportBackStackChanged();
        }
    }

    private void makeRemovedFragmentsInvisible(ArraySet<Fragment> fragments) {
        int numAdded = fragments.size();
        for (int i = 0; i < numAdded; i++) {
            Fragment fragment = (Fragment) fragments.valueAt(i);
            if (!fragment.mAdded) {
                View view = fragment.getView();
                fragment.mPostponedAlpha = view.getAlpha();
                view.setAlpha(0.0f);
            }
        }
    }

    private int postponePostponableTransactions(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex, ArraySet<Fragment> added) {
        int postponeIndex = endIndex;
        for (int i = endIndex - 1; i >= startIndex; i--) {
            BackStackRecord record = (BackStackRecord) records.get(i);
            boolean isPop = ((Boolean) isRecordPop.get(i)).booleanValue();
            if (record.isPostponed() && !record.interactsWith(records, i + 1, endIndex)) {
                if (this.mPostponedTransactions == null) {
                    this.mPostponedTransactions = new ArrayList<>();
                }
                StartEnterTransitionListener listener = new StartEnterTransitionListener(record, isPop);
                this.mPostponedTransactions.add(listener);
                record.setOnStartPostponedListener(listener);
                if (isPop) {
                    record.executeOps();
                } else {
                    record.executePopOps(false);
                }
                postponeIndex--;
                if (i != postponeIndex) {
                    records.remove(i);
                    records.add(postponeIndex, record);
                }
                addAddedFragments(added);
            }
        }
        return postponeIndex;
    }

    /* access modifiers changed from: private */
    public void completeExecute(BackStackRecord record, boolean isPop, boolean runTransitions, boolean moveToState) {
        if (isPop) {
            record.executePopOps(moveToState);
        } else {
            record.executeOps();
        }
        ArrayList<BackStackRecord> records = new ArrayList<>(1);
        ArrayList arrayList = new ArrayList(1);
        records.add(record);
        arrayList.add(Boolean.valueOf(isPop));
        if (runTransitions) {
            FragmentTransition.startTransitions(this, records, arrayList, 0, 1, true);
        }
        if (moveToState) {
            moveToState(this.mCurState, true);
        }
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray != null) {
            int numActive = sparseArray.size();
            for (int i = 0; i < numActive; i++) {
                Fragment fragment = (Fragment) this.mActive.valueAt(i);
                if (fragment != null && fragment.mView != null && fragment.mIsNewlyAdded && record.interactsWith(fragment.mContainerId)) {
                    if (fragment.mPostponedAlpha > 0.0f) {
                        fragment.mView.setAlpha(fragment.mPostponedAlpha);
                    }
                    if (moveToState) {
                        fragment.mPostponedAlpha = 0.0f;
                    } else {
                        fragment.mPostponedAlpha = -1.0f;
                        fragment.mIsNewlyAdded = false;
                    }
                }
            }
        }
    }

    private Fragment findFragmentUnder(Fragment f) {
        ViewGroup container = f.mContainer;
        View view = f.mView;
        if (container == null || view == null) {
            return null;
        }
        for (int i = this.mAdded.indexOf(f) - 1; i >= 0; i--) {
            Fragment underFragment = (Fragment) this.mAdded.get(i);
            if (underFragment.mContainer == container && underFragment.mView != null) {
                return underFragment;
            }
        }
        return null;
    }

    private static void executeOps(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, int startIndex, int endIndex) {
        for (int i = startIndex; i < endIndex; i++) {
            BackStackRecord record = (BackStackRecord) records.get(i);
            boolean moveToState = true;
            if (((Boolean) isRecordPop.get(i)).booleanValue()) {
                record.bumpBackStackNesting(-1);
                if (i != endIndex - 1) {
                    moveToState = false;
                }
                record.executePopOps(moveToState);
            } else {
                record.bumpBackStackNesting(1);
                record.executeOps();
            }
        }
    }

    private void addAddedFragments(ArraySet<Fragment> added) {
        int i = this.mCurState;
        if (i >= 1) {
            int state = Math.min(i, 4);
            int numAdded = this.mAdded.size();
            for (int i2 = 0; i2 < numAdded; i2++) {
                Fragment fragment = (Fragment) this.mAdded.get(i2);
                if (fragment.mState < state) {
                    moveToState(fragment, state, fragment.getNextAnim(), fragment.getNextTransition(), false);
                    if (fragment.mView != null && !fragment.mHidden && fragment.mIsNewlyAdded) {
                        added.add(fragment);
                    }
                }
            }
        }
    }

    private void forcePostponedTransactions() {
        if (this.mPostponedTransactions != null) {
            while (!this.mPostponedTransactions.isEmpty()) {
                ((StartEnterTransitionListener) this.mPostponedTransactions.remove(0)).completeTransaction();
            }
        }
    }

    private void endAnimatingAwayFragments() {
        SparseArray<Fragment> sparseArray = this.mActive;
        int numFragments = sparseArray == null ? 0 : sparseArray.size();
        for (int i = 0; i < numFragments; i++) {
            Fragment fragment = (Fragment) this.mActive.valueAt(i);
            if (fragment != null) {
                if (fragment.getAnimatingAway() != null) {
                    int stateAfterAnimating = fragment.getStateAfterAnimating();
                    View animatingAway = fragment.getAnimatingAway();
                    Animation animation = animatingAway.getAnimation();
                    if (animation != null) {
                        animation.cancel();
                        animatingAway.clearAnimation();
                    }
                    fragment.setAnimatingAway(null);
                    moveToState(fragment, stateAfterAnimating, 0, 0, false);
                } else if (fragment.getAnimator() != null) {
                    fragment.getAnimator().end();
                }
            }
        }
    }

    private boolean generateOpsForPendingActions(ArrayList<BackStackRecord> records, ArrayList<Boolean> isPop) {
        boolean didSomething = false;
        synchronized (this) {
            if (this.mPendingActions != null) {
                if (this.mPendingActions.size() != 0) {
                    for (int i = 0; i < this.mPendingActions.size(); i++) {
                        didSomething |= ((OpGenerator) this.mPendingActions.get(i)).generateOps(records, isPop);
                    }
                    this.mPendingActions.clear();
                    this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                    return didSomething;
                }
            }
            return false;
        }
    }

    /* access modifiers changed from: 0000 */
    public void doPendingDeferredStart() {
        if (this.mHavePendingDeferredStart) {
            this.mHavePendingDeferredStart = false;
            startPendingDeferredFragments();
        }
    }

    /* access modifiers changed from: 0000 */
    public void reportBackStackChanged() {
        if (this.mBackStackChangeListeners != null) {
            for (int i = 0; i < this.mBackStackChangeListeners.size(); i++) {
                ((OnBackStackChangedListener) this.mBackStackChangeListeners.get(i)).onBackStackChanged();
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void addBackStackState(BackStackRecord state) {
        if (this.mBackStack == null) {
            this.mBackStack = new ArrayList<>();
        }
        this.mBackStack.add(state);
    }

    /* access modifiers changed from: 0000 */
    public boolean popBackStackState(ArrayList<BackStackRecord> records, ArrayList<Boolean> isRecordPop, String name, int id, int flags) {
        ArrayList<BackStackRecord> arrayList = this.mBackStack;
        if (arrayList == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & 1) == 0) {
            int last = arrayList.size() - 1;
            if (last < 0) {
                return false;
            }
            records.add(this.mBackStack.remove(last));
            isRecordPop.add(Boolean.valueOf(true));
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                int index2 = this.mBackStack.size() - 1;
                while (index >= 0) {
                    BackStackRecord bss = (BackStackRecord) this.mBackStack.get(index);
                    if ((name != null && name.equals(bss.getName())) || (id >= 0 && id == bss.mIndex)) {
                        break;
                    }
                    index2 = index - 1;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags & 1) != 0) {
                    index--;
                    while (index >= 0) {
                        BackStackRecord bss2 = (BackStackRecord) this.mBackStack.get(index);
                        if ((name == null || !name.equals(bss2.getName())) && (id < 0 || id != bss2.mIndex)) {
                            break;
                        }
                        index--;
                    }
                }
            }
            if (index == this.mBackStack.size() - 1) {
                return false;
            }
            for (int i = this.mBackStack.size() - 1; i > index; i--) {
                records.add(this.mBackStack.remove(i));
                isRecordPop.add(Boolean.valueOf(true));
            }
        }
        return true;
    }

    /* access modifiers changed from: 0000 */
    public FragmentManagerNonConfig retainNonConfig() {
        setRetaining(this.mSavedNonConfig);
        return this.mSavedNonConfig;
    }

    private static void setRetaining(FragmentManagerNonConfig nonConfig) {
        if (nonConfig != null) {
            List<Fragment> fragments = nonConfig.getFragments();
            if (fragments != null) {
                for (Fragment fragment : fragments) {
                    fragment.mRetaining = true;
                }
            }
            List<FragmentManagerNonConfig> children = nonConfig.getChildNonConfigs();
            if (children != null) {
                for (FragmentManagerNonConfig child : children) {
                    setRetaining(child);
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void saveNonConfig() {
        FragmentManagerNonConfig child;
        ArrayList arrayList = null;
        ArrayList arrayList2 = null;
        ArrayList arrayList3 = null;
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = (Fragment) this.mActive.valueAt(i);
                if (f != null) {
                    if (f.mRetainInstance) {
                        if (arrayList == null) {
                            arrayList = new ArrayList();
                        }
                        arrayList.add(f);
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("retainNonConfig: keeping retained ");
                            sb.append(f);
                            Log.v(TAG, sb.toString());
                        }
                    }
                    if (f.mChildFragmentManager != null) {
                        f.mChildFragmentManager.saveNonConfig();
                        child = f.mChildFragmentManager.mSavedNonConfig;
                    } else {
                        child = f.mChildNonConfig;
                    }
                    if (arrayList2 == null && child != null) {
                        arrayList2 = new ArrayList(this.mActive.size());
                        for (int j = 0; j < i; j++) {
                            arrayList2.add(null);
                        }
                    }
                    if (arrayList2 != null) {
                        arrayList2.add(child);
                    }
                    if (arrayList3 == null && f.mViewModelStore != null) {
                        arrayList3 = new ArrayList(this.mActive.size());
                        for (int j2 = 0; j2 < i; j2++) {
                            arrayList3.add(null);
                        }
                    }
                    if (arrayList3 != null) {
                        arrayList3.add(f.mViewModelStore);
                    }
                }
            }
        }
        if (arrayList == null && arrayList2 == null && arrayList3 == null) {
            this.mSavedNonConfig = null;
        } else {
            this.mSavedNonConfig = new FragmentManagerNonConfig(arrayList, arrayList2, arrayList3);
        }
    }

    /* access modifiers changed from: 0000 */
    public void saveFragmentViewState(Fragment f) {
        if (f.mInnerView != null) {
            SparseArray<Parcelable> sparseArray = this.mStateArray;
            if (sparseArray == null) {
                this.mStateArray = new SparseArray<>();
            } else {
                sparseArray.clear();
            }
            f.mInnerView.saveHierarchyState(this.mStateArray);
            if (this.mStateArray.size() > 0) {
                f.mSavedViewState = this.mStateArray;
                this.mStateArray = null;
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = null;
        if (this.mStateBundle == null) {
            this.mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(this.mStateBundle);
        dispatchOnFragmentSaveInstanceState(f, this.mStateBundle, false);
        if (!this.mStateBundle.isEmpty()) {
            result = this.mStateBundle;
            this.mStateBundle = null;
        }
        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            result.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }
        return result;
    }

    /* access modifiers changed from: 0000 */
    public Parcelable saveAllState() {
        String str;
        String str2;
        String str3;
        String str4;
        forcePostponedTransactions();
        endAnimatingAwayFragments();
        execPendingActions();
        this.mStateSaved = true;
        this.mSavedNonConfig = null;
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray == null || sparseArray.size() <= 0) {
            return null;
        }
        int N = this.mActive.size();
        FragmentState[] active = new FragmentState[N];
        boolean haveFragments = false;
        int i = 0;
        while (true) {
            str = " has cleared index: ";
            str2 = "Failure saving state: active ";
            str3 = ": ";
            str4 = TAG;
            if (i >= N) {
                break;
            }
            Fragment f = (Fragment) this.mActive.valueAt(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(str2);
                    sb.append(f);
                    sb.append(str);
                    sb.append(f.mIndex);
                    throwException(new IllegalStateException(sb.toString()));
                }
                haveFragments = true;
                FragmentState fs = new FragmentState(f);
                active[i] = fs;
                if (f.mState <= 0 || fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                } else {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);
                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Failure saving state: ");
                            sb2.append(f);
                            sb2.append(" has target not in fragment manager: ");
                            sb2.append(f.mTarget);
                            throwException(new IllegalStateException(sb2.toString()));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState, TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
                        }
                    }
                }
                if (DEBUG) {
                    StringBuilder sb3 = new StringBuilder();
                    sb3.append("Saved state of ");
                    sb3.append(f);
                    sb3.append(str3);
                    sb3.append(fs.mSavedFragmentState);
                    Log.v(str4, sb3.toString());
                }
            }
            i++;
        }
        if (!haveFragments) {
            if (DEBUG) {
                Log.v(str4, "saveAllState: no fragments!");
            }
            return null;
        }
        int[] added = null;
        BackStackState[] backStack = null;
        int N2 = this.mAdded.size();
        if (N2 > 0) {
            added = new int[N2];
            for (int i2 = 0; i2 < N2; i2++) {
                added[i2] = ((Fragment) this.mAdded.get(i2)).mIndex;
                if (added[i2] < 0) {
                    StringBuilder sb4 = new StringBuilder();
                    sb4.append(str2);
                    sb4.append(this.mAdded.get(i2));
                    sb4.append(str);
                    sb4.append(added[i2]);
                    throwException(new IllegalStateException(sb4.toString()));
                }
                if (DEBUG) {
                    StringBuilder sb5 = new StringBuilder();
                    sb5.append("saveAllState: adding fragment #");
                    sb5.append(i2);
                    sb5.append(str3);
                    sb5.append(this.mAdded.get(i2));
                    Log.v(str4, sb5.toString());
                }
            }
        }
        ArrayList<BackStackRecord> arrayList = this.mBackStack;
        if (arrayList != null) {
            int N3 = arrayList.size();
            if (N3 > 0) {
                backStack = new BackStackState[N3];
                for (int i3 = 0; i3 < N3; i3++) {
                    backStack[i3] = new BackStackState((BackStackRecord) this.mBackStack.get(i3));
                    if (DEBUG) {
                        StringBuilder sb6 = new StringBuilder();
                        sb6.append("saveAllState: adding back stack #");
                        sb6.append(i3);
                        sb6.append(str3);
                        sb6.append(this.mBackStack.get(i3));
                        Log.v(str4, sb6.toString());
                    }
                }
            }
        }
        FragmentManagerState fms = new FragmentManagerState();
        fms.mActive = active;
        fms.mAdded = added;
        fms.mBackStack = backStack;
        Fragment fragment = this.mPrimaryNav;
        if (fragment != null) {
            fms.mPrimaryNavActiveIndex = fragment.mIndex;
        }
        fms.mNextFragmentIndex = this.mNextFragmentIndex;
        saveNonConfig();
        return fms;
    }

    /* access modifiers changed from: 0000 */
    public void restoreAllState(Parcelable state, FragmentManagerNonConfig nonConfig) {
        List list;
        List list2;
        FragmentManagerNonConfig childNonConfig;
        ViewModelStore viewModelStore;
        if (state != null) {
            FragmentManagerState fms = (FragmentManagerState) state;
            if (fms.mActive != null) {
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments = nonConfig.getFragments();
                    List<FragmentManagerNonConfig> childNonConfigs = nonConfig.getChildNonConfigs();
                    List<ViewModelStore> viewModelStores = nonConfig.getViewModelStores();
                    int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
                    for (int i = 0; i < count; i++) {
                        Fragment f = (Fragment) nonConfigFragments.get(i);
                        if (DEBUG) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("restoreAllState: re-attaching retained ");
                            sb.append(f);
                            Log.v(TAG, sb.toString());
                        }
                        int index = 0;
                        while (index < fms.mActive.length && fms.mActive[index].mIndex != f.mIndex) {
                            index++;
                        }
                        if (index == fms.mActive.length) {
                            StringBuilder sb2 = new StringBuilder();
                            sb2.append("Could not find active fragment with index ");
                            sb2.append(f.mIndex);
                            throwException(new IllegalStateException(sb2.toString()));
                        }
                        FragmentState fs = fms.mActive[index];
                        fs.mInstance = f;
                        f.mSavedViewState = null;
                        f.mBackStackNesting = 0;
                        f.mInLayout = false;
                        f.mAdded = false;
                        f.mTarget = null;
                        if (fs.mSavedFragmentState != null) {
                            fs.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                            f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            f.mSavedFragmentState = fs.mSavedFragmentState;
                        }
                    }
                    list = viewModelStores;
                    list2 = childNonConfigs;
                } else {
                    list = null;
                    list2 = null;
                }
                this.mActive = new SparseArray<>(fms.mActive.length);
                for (int i2 = 0; i2 < fms.mActive.length; i2++) {
                    FragmentState fs2 = fms.mActive[i2];
                    if (fs2 != null) {
                        if (list2 == null || i2 >= list2.size()) {
                            childNonConfig = null;
                        } else {
                            childNonConfig = (FragmentManagerNonConfig) list2.get(i2);
                        }
                        if (list == null || i2 >= list.size()) {
                            viewModelStore = null;
                        } else {
                            viewModelStore = (ViewModelStore) list.get(i2);
                        }
                        Fragment f2 = fs2.instantiate(this.mHost, this.mContainer, this.mParent, childNonConfig, viewModelStore);
                        if (DEBUG) {
                            StringBuilder sb3 = new StringBuilder();
                            sb3.append("restoreAllState: active #");
                            sb3.append(i2);
                            sb3.append(": ");
                            sb3.append(f2);
                            Log.v(TAG, sb3.toString());
                        }
                        this.mActive.put(f2.mIndex, f2);
                        fs2.mInstance = null;
                    }
                }
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments2 = nonConfig.getFragments();
                    int count2 = nonConfigFragments2 != null ? nonConfigFragments2.size() : 0;
                    for (int i3 = 0; i3 < count2; i3++) {
                        Fragment f3 = (Fragment) nonConfigFragments2.get(i3);
                        if (f3.mTargetIndex >= 0) {
                            f3.mTarget = (Fragment) this.mActive.get(f3.mTargetIndex);
                            if (f3.mTarget == null) {
                                StringBuilder sb4 = new StringBuilder();
                                sb4.append("Re-attaching retained fragment ");
                                sb4.append(f3);
                                sb4.append(" target no longer exists: ");
                                sb4.append(f3.mTargetIndex);
                                Log.w(TAG, sb4.toString());
                            }
                        }
                    }
                }
                this.mAdded.clear();
                if (fms.mAdded != null) {
                    int i4 = 0;
                    while (i4 < fms.mAdded.length) {
                        Fragment f4 = (Fragment) this.mActive.get(fms.mAdded[i4]);
                        if (f4 == null) {
                            StringBuilder sb5 = new StringBuilder();
                            sb5.append("No instantiated fragment for index #");
                            sb5.append(fms.mAdded[i4]);
                            throwException(new IllegalStateException(sb5.toString()));
                        }
                        f4.mAdded = true;
                        if (DEBUG) {
                            StringBuilder sb6 = new StringBuilder();
                            sb6.append("restoreAllState: added #");
                            sb6.append(i4);
                            sb6.append(": ");
                            sb6.append(f4);
                            Log.v(TAG, sb6.toString());
                        }
                        if (!this.mAdded.contains(f4)) {
                            synchronized (this.mAdded) {
                                this.mAdded.add(f4);
                            }
                            i4++;
                        } else {
                            throw new IllegalStateException("Already added!");
                        }
                    }
                }
                if (fms.mBackStack != null) {
                    this.mBackStack = new ArrayList<>(fms.mBackStack.length);
                    for (int i5 = 0; i5 < fms.mBackStack.length; i5++) {
                        BackStackRecord bse = fms.mBackStack[i5].instantiate(this);
                        if (DEBUG) {
                            StringBuilder sb7 = new StringBuilder();
                            sb7.append("restoreAllState: back stack #");
                            sb7.append(i5);
                            sb7.append(" (index ");
                            sb7.append(bse.mIndex);
                            sb7.append("): ");
                            sb7.append(bse);
                            Log.v(TAG, sb7.toString());
                            PrintWriter pw = new PrintWriter(new LogWriter(TAG));
                            bse.dump("  ", pw, false);
                            pw.close();
                        }
                        this.mBackStack.add(bse);
                        if (bse.mIndex >= 0) {
                            setBackStackIndex(bse.mIndex, bse);
                        }
                    }
                } else {
                    this.mBackStack = null;
                }
                if (fms.mPrimaryNavActiveIndex >= 0) {
                    this.mPrimaryNav = (Fragment) this.mActive.get(fms.mPrimaryNavActiveIndex);
                }
                this.mNextFragmentIndex = fms.mNextFragmentIndex;
            }
        }
    }

    private void burpActive() {
        SparseArray<Fragment> sparseArray = this.mActive;
        if (sparseArray != null) {
            for (int i = sparseArray.size() - 1; i >= 0; i--) {
                if (this.mActive.valueAt(i) == null) {
                    SparseArray<Fragment> sparseArray2 = this.mActive;
                    sparseArray2.delete(sparseArray2.keyAt(i));
                }
            }
        }
    }

    public void attachController(FragmentHostCallback host, FragmentContainer container, Fragment parent) {
        if (this.mHost == null) {
            this.mHost = host;
            this.mContainer = container;
            this.mParent = parent;
            return;
        }
        throw new IllegalStateException("Already attached");
    }

    public void noteStateNotSaved() {
        this.mSavedNonConfig = null;
        this.mStateSaved = false;
        this.mStopped = false;
        int addedCount = this.mAdded.size();
        for (int i = 0; i < addedCount; i++) {
            Fragment fragment = (Fragment) this.mAdded.get(i);
            if (fragment != null) {
                fragment.noteStateNotSaved();
            }
        }
    }

    public void dispatchCreate() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(1);
    }

    public void dispatchActivityCreated() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(2);
    }

    public void dispatchStart() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(4);
    }

    public void dispatchResume() {
        this.mStateSaved = false;
        this.mStopped = false;
        dispatchStateChange(5);
    }

    public void dispatchPause() {
        dispatchStateChange(4);
    }

    public void dispatchStop() {
        this.mStopped = true;
        dispatchStateChange(3);
    }

    public void dispatchReallyStop() {
        dispatchStateChange(2);
    }

    public void dispatchDestroyView() {
        dispatchStateChange(1);
    }

    public void dispatchDestroy() {
        this.mDestroyed = true;
        execPendingActions();
        dispatchStateChange(0);
        this.mHost = null;
        this.mContainer = null;
        this.mParent = null;
    }

    /* JADX INFO: finally extract failed */
    private void dispatchStateChange(int nextState) {
        try {
            this.mExecutingActions = true;
            moveToState(nextState, false);
            this.mExecutingActions = false;
            execPendingActions();
        } catch (Throwable th) {
            this.mExecutingActions = false;
            throw th;
        }
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performMultiWindowModeChanged(isInMultiWindowMode);
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        for (int i = this.mAdded.size() - 1; i >= 0; i--) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performPictureInPictureModeChanged(isInPictureInPictureMode);
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performConfigurationChanged(newConfig);
            }
        }
    }

    public void dispatchLowMemory() {
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null) {
                f.performLowMemory();
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (this.mCurState < 1) {
            return false;
        }
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performCreateOptionsMenu(menu, inflater)) {
                show = true;
                if (newMenus == null) {
                    newMenus = new ArrayList<>();
                }
                newMenus.add(f);
            }
        }
        if (this.mCreatedMenus != null) {
            for (int i2 = 0; i2 < this.mCreatedMenus.size(); i2++) {
                Fragment f2 = (Fragment) this.mCreatedMenus.get(i2);
                if (newMenus == null || !newMenus.contains(f2)) {
                    f2.onDestroyOptionsMenu();
                }
            }
        }
        this.mCreatedMenus = newMenus;
        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        if (this.mCurState < 1) {
            return false;
        }
        boolean show = false;
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performPrepareOptionsMenu(menu)) {
                show = true;
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (this.mCurState < 1) {
            return false;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performOptionsItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (this.mCurState < 1) {
            return false;
        }
        for (int i = 0; i < this.mAdded.size(); i++) {
            Fragment f = (Fragment) this.mAdded.get(i);
            if (f != null && f.performContextItemSelected(item)) {
                return true;
            }
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (this.mCurState >= 1) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = (Fragment) this.mAdded.get(i);
                if (f != null) {
                    f.performOptionsMenuClosed(menu);
                }
            }
        }
    }

    public void setPrimaryNavigationFragment(Fragment f) {
        if (f == null || (this.mActive.get(f.mIndex) == f && (f.mHost == null || f.getFragmentManager() == this))) {
            this.mPrimaryNav = f;
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Fragment ");
        sb.append(f);
        sb.append(" is not an active fragment of FragmentManager ");
        sb.append(this);
        throw new IllegalArgumentException(sb.toString());
    }

    public Fragment getPrimaryNavigationFragment() {
        return this.mPrimaryNav;
    }

    public void registerFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb, boolean recursive) {
        this.mLifecycleCallbacks.add(new Pair(cb, Boolean.valueOf(recursive)));
    }

    public void unregisterFragmentLifecycleCallbacks(FragmentLifecycleCallbacks cb) {
        synchronized (this.mLifecycleCallbacks) {
            int i = 0;
            int N = this.mLifecycleCallbacks.size();
            while (true) {
                if (i >= N) {
                    break;
                } else if (((Pair) this.mLifecycleCallbacks.get(i)).first == cb) {
                    this.mLifecycleCallbacks.remove(i);
                    break;
                } else {
                    i++;
                }
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentPreAttached(Fragment f, Context context, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPreAttached(this, f, context);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentAttached(Fragment f, Context context, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentAttached(f, context, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentAttached(this, f, context);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentPreCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPreCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPreCreated(this, f, savedInstanceState);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentCreated(this, f, savedInstanceState);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentActivityCreated(Fragment f, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentActivityCreated(f, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentActivityCreated(this, f, savedInstanceState);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentViewCreated(Fragment f, View v, Bundle savedInstanceState, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewCreated(f, v, savedInstanceState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentViewCreated(this, f, v, savedInstanceState);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentStarted(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStarted(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentStarted(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentResumed(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentResumed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentResumed(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentPaused(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentPaused(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentPaused(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentStopped(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentStopped(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentStopped(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentSaveInstanceState(Fragment f, Bundle outState, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentSaveInstanceState(f, outState, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentSaveInstanceState(this, f, outState);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentViewDestroyed(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentViewDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentViewDestroyed(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentDestroyed(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDestroyed(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentDestroyed(this, f);
            }
        }
    }

    /* access modifiers changed from: 0000 */
    public void dispatchOnFragmentDetached(Fragment f, boolean onlyRecursive) {
        Fragment fragment = this.mParent;
        if (fragment != null) {
            FragmentManager parentManager = fragment.getFragmentManager();
            if (parentManager instanceof FragmentManagerImpl) {
                ((FragmentManagerImpl) parentManager).dispatchOnFragmentDetached(f, true);
            }
        }
        Iterator it = this.mLifecycleCallbacks.iterator();
        while (it.hasNext()) {
            Pair<FragmentLifecycleCallbacks, Boolean> p = (Pair) it.next();
            if (!onlyRecursive || ((Boolean) p.second).booleanValue()) {
                ((FragmentLifecycleCallbacks) p.first).onFragmentDetached(this, f);
            }
        }
    }

    public static int reverseTransit(int transit) {
        if (transit == 4097) {
            return 8194;
        }
        if (transit == 4099) {
            return FragmentTransaction.TRANSIT_FRAGMENT_FADE;
        }
        if (transit != 8194) {
            return 0;
        }
        return FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        if (transit == 4097) {
            return enter ? 1 : 2;
        } else if (transit == 4099) {
            return enter ? 5 : 6;
        } else if (transit != 8194) {
            return -1;
        } else {
            return enter ? 3 : 4;
        }
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        String fname;
        Fragment fragment;
        Context context2 = context;
        AttributeSet attributeSet = attrs;
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname2 = attributeSet.getAttributeValue(null, "class");
        TypedArray a = context2.obtainStyledAttributes(attributeSet, FragmentTag.Fragment);
        int i = 0;
        if (fname2 == null) {
            fname = a.getString(0);
        } else {
            fname = fname2;
        }
        int id = a.getResourceId(1, -1);
        String tag = a.getString(2);
        a.recycle();
        if (!Fragment.isSupportFragmentClass(this.mHost.getContext(), fname)) {
            return null;
        }
        if (parent != null) {
            i = parent.getId();
        }
        int containerId = i;
        if (containerId == -1 && id == -1 && tag == null) {
            StringBuilder sb = new StringBuilder();
            sb.append(attrs.getPositionDescription());
            sb.append(": Must specify unique android:id, android:tag, or have a parent with an id for ");
            sb.append(fname);
            throw new IllegalArgumentException(sb.toString());
        }
        Fragment fragment2 = id != -1 ? findFragmentById(id) : null;
        if (fragment2 == null && tag != null) {
            fragment2 = findFragmentByTag(tag);
        }
        if (fragment2 == null && containerId != -1) {
            fragment2 = findFragmentById(containerId);
        }
        if (DEBUG) {
            StringBuilder sb2 = new StringBuilder();
            sb2.append("onCreateView: id=0x");
            sb2.append(Integer.toHexString(id));
            sb2.append(" fname=");
            sb2.append(fname);
            sb2.append(" existing=");
            sb2.append(fragment2);
            Log.v(TAG, sb2.toString());
        }
        if (fragment2 == null) {
            Fragment fragment3 = this.mContainer.instantiate(context2, fname, null);
            fragment3.mFromLayout = true;
            fragment3.mFragmentId = id != 0 ? id : containerId;
            fragment3.mContainerId = containerId;
            fragment3.mTag = tag;
            fragment3.mInLayout = true;
            fragment3.mFragmentManager = this;
            FragmentHostCallback fragmentHostCallback = this.mHost;
            fragment3.mHost = fragmentHostCallback;
            fragment3.onInflate(fragmentHostCallback.getContext(), attributeSet, fragment3.mSavedFragmentState);
            addFragment(fragment3, true);
            fragment = fragment3;
        } else if (!fragment2.mInLayout) {
            fragment2.mInLayout = true;
            fragment2.mHost = this.mHost;
            if (!fragment2.mRetaining) {
                fragment2.onInflate(this.mHost.getContext(), attributeSet, fragment2.mSavedFragmentState);
            }
            fragment = fragment2;
        } else {
            StringBuilder sb3 = new StringBuilder();
            sb3.append(attrs.getPositionDescription());
            sb3.append(": Duplicate id 0x");
            sb3.append(Integer.toHexString(id));
            sb3.append(", tag ");
            sb3.append(tag);
            sb3.append(", or parent id 0x");
            sb3.append(Integer.toHexString(containerId));
            sb3.append(" with another fragment for ");
            sb3.append(fname);
            throw new IllegalArgumentException(sb3.toString());
        }
        if (this.mCurState >= 1 || !fragment.mFromLayout) {
            moveToState(fragment);
        } else {
            moveToState(fragment, 1, 0, 0, false);
        }
        if (fragment.mView != null) {
            if (id != 0) {
                fragment.mView.setId(id);
            }
            if (fragment.mView.getTag() == null) {
                fragment.mView.setTag(tag);
            }
            return fragment.mView;
        }
        StringBuilder sb4 = new StringBuilder();
        sb4.append("Fragment ");
        sb4.append(fname);
        sb4.append(" did not create a view.");
        throw new IllegalStateException(sb4.toString());
    }

    public View onCreateView(String name, Context context, AttributeSet attrs) {
        return onCreateView(null, name, context, attrs);
    }

    /* access modifiers changed from: 0000 */
    public Factory2 getLayoutInflaterFactory() {
        return this;
    }
}
