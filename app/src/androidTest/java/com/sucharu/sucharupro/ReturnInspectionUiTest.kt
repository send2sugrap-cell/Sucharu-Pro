package com.sucharu.sucharupro

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Minimal UI test that verifies the Module 11 Step 03 Return Inspection flow.
 *
 * The test assumes that the app's main activity hosts the Navigation Compose graph
 * and that the relevant UI elements contain identifiable text strings.
 * It navigates through the following screens:
 *   - Return List
 *   - Return Request Details
 *   - Under Inspection (detail view)
 *   - Return Inspection screen
 *   - Checklist screen
 *   - Approve / Reject actions
 *
 * This test uses the AndroidComposeRule to launch the activity and perform clicks
 * based on node text. Adjust the text selectors if the actual UI uses different labels
 * or test tags.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class ReturnInspectionUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun verifyReturnInspectionFlow() {
        // Verify Return List screen is displayed
        composeTestRule.onNodeWithText("Return List").assertExists()
        // Open first return request (assumes the list has an item with text "Return #1")
        composeTestRule.onNodeWithText("Return #1").performClick()

        // Verify Return Request Details screen
        composeTestRule.onNodeWithText("Return Request Details").assertExists()
        // Navigate to UNDER_INSPECTION (assumes a button with this label)
        composeTestRule.onNodeWithText("UNDER_INSPECTION").performClick()

        // Verify Return Inspection screen
        composeTestRule.onNodeWithText("Return Inspection").assertExists()
        // Open Checklist (assumes a button or tab with label "Checklist")
        composeTestRule.onNodeWithText("Checklist").performClick()

        // Verify Checklist is displayed
        composeTestRule.onNodeWithText("Checklist").assertExists()
        // Perform an approve action (assumes a button labeled "Approve")
        composeTestRule.onNodeWithText("Approve").performClick()
        // Verify the status changes to APPROVED (could be a snackbar or label)
        composeTestRule.onNodeWithText("APPROVED").assertExists()

        // Navigate back to the inspection screen and test reject path as well
        composeTestRule.onNodeWithText("Back").performClick()
        composeTestRule.onNodeWithText("Reject").performClick()
        composeTestRule.onNodeWithText("REJECTED").assertExists()
    }
}
