@file:RestrictTo(RestrictTo.Scope.LIBRARY)

package com.stripe.android.paymentsheet.ui

import androidx.annotation.RestrictTo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.stripe.android.common.ui.PrimaryButton
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.uicore.StripeTheme
import com.stripe.android.uicore.elements.DROPDOWN_MENU_CLICKABLE_TEST_TAG
import com.stripe.android.uicore.elements.SectionCard
import com.stripe.android.uicore.elements.SingleChoiceDropdown
import com.stripe.android.uicore.elements.TextFieldColors
import com.stripe.android.uicore.stripeColors
import com.stripe.android.R as PaymentsCoreR
import com.stripe.android.R as StripeR
import com.stripe.android.uicore.R as UiCoreR

@Composable
internal fun EditPaymentMethod(
    interactor: EditPaymentMethodViewInteractor,
    modifier: Modifier = Modifier
) {
    val viewState by interactor.viewState.collectAsState()

    EditPaymentMethodUi(
        modifier = modifier,
        viewState = viewState,
        viewActionHandler = interactor::handleViewAction
    )
}

@Composable
internal fun EditPaymentMethodUi(
    viewState: EditPaymentMethodViewState,
    viewActionHandler: (action: EditPaymentMethodViewAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = dimensionResource(id = R.dimen.stripe_paymentsheet_outer_spacing_horizontal)

    Column(
        modifier = modifier.padding(
            top = padding,
            start = padding,
            end = padding
        )
    ) {
        SectionCard {
            val colors = TextFieldColors(false)

            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = "•••• •••• •••• ${viewState.last4}",
                enabled = false,
                colors = colors,
                label = {
                    Text(
                        text = stringResource(id = StripeR.string.stripe_acc_label_card_number),
                        modifier = modifier,
                        color = MaterialTheme.stripeColors.placeholderText
                            .copy(alpha = ContentAlpha.disabled),
                        style = MaterialTheme.typography.subtitle1
                    )
                },
                trailingIcon = {
                    Dropdown(viewState, viewActionHandler)
                },
                onValueChange = {}
            )
        }

        Spacer(modifier = Modifier.requiredHeight(48.dp))

        PrimaryButton(
            label = stringResource(id = StripeR.string.stripe_title_update_card),
            isLoading = viewState.status == EditPaymentMethodViewState.Status.Updating,
            isEnabled = viewState.canUpdate && viewState.status == EditPaymentMethodViewState.Status.Idle,
            onButtonClick = {
                viewActionHandler.invoke(EditPaymentMethodViewAction.OnUpdatePressed)
            }
        )

        Spacer(modifier = Modifier.requiredHeight(8.dp))

        PrimaryButton(
            label = stringResource(id = R.string.stripe_paymentsheet_remove_card),
            isLoading = viewState.status == EditPaymentMethodViewState.Status.Removing,
            isEnabled = viewState.status == EditPaymentMethodViewState.Status.Idle,
            onButtonClick = {
                viewActionHandler.invoke(EditPaymentMethodViewAction.OnRemovePressed)
            }
        )
    }
}

@Composable
private fun Dropdown(
    viewState: EditPaymentMethodViewState,
    viewActionHandler: (action: EditPaymentMethodViewAction) -> Unit
) {
    var expanded by remember {
        mutableStateOf(false)
    }

    Box {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .testTag(DROPDOWN_MENU_CLICKABLE_TEST_TAG)
                .clickable {
                    expanded = true
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Image(
                painter = painterResource(id = viewState.selectedBrand.icon),
                contentDescription = null
            )

            Icon(
                painter = painterResource(
                    id = UiCoreR.drawable.stripe_ic_chevron_down
                ),
                contentDescription = null
            )
        }

        SingleChoiceDropdown(
            expanded = expanded,
            title = resolvableString(
                id = PaymentsCoreR.string.stripe_card_brand_choice_selection_header
            ),
            currentChoice = viewState.selectedBrand,
            choices = viewState.availableBrands,
            headerTextColor = MaterialTheme.stripeColors.subtitle,
            optionTextColor = MaterialTheme.stripeColors.onComponent,
            onChoiceSelected = { item ->
                expanded = false

                viewActionHandler.invoke(
                    EditPaymentMethodViewAction.OnBrandChoiceChanged(item)
                )
            },
            onDismiss = {
                expanded = false
            }
        )
    }
}

@Composable
@Preview(showBackground = true)
private fun EditPaymentMethodPreview() {
    StripeTheme {
        EditPaymentMethodUi(
            viewState = EditPaymentMethodViewState(
                status = EditPaymentMethodViewState.Status.Idle,
                last4 = "4242",
                selectedBrand = EditPaymentMethodViewState.CardBrandChoice(
                    brand = CardBrand.CartesBancaires
                ),
                canUpdate = true,
                availableBrands = listOf(
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.Visa
                    ),
                    EditPaymentMethodViewState.CardBrandChoice(
                        brand = CardBrand.CartesBancaires
                    )
                )
            ),
            viewActionHandler = {}
        )
    }
}