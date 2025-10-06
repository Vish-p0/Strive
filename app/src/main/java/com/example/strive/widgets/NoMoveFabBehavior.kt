package com.example.strive.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * A no-op behavior for FloatingActionButton that keeps it fixed in place.
 * It ignores Snackbars and nested scroll changes so the FAB won't translate/move.
 */
class NoMoveFabBehavior() : CoordinatorLayout.Behavior<FloatingActionButton>() {

    constructor(context: Context, attrs: AttributeSet) : this()

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        // Do not depend on any sibling views (e.g., Snackbar), so we never adjust position
        return false
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: FloatingActionButton,
        dependency: View
    ): Boolean {
        // Never react to dependency changes
        return false
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: FloatingActionButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        // Do not participate in nested scroll (prevents auto-hide/move)
        return false
    }
}
