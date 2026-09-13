package com.thevirtualtrust.ppis.feature.auth.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thevirtualtrust.ppis.feature.auth.ForgotPasswordScreen
import com.thevirtualtrust.ppis.feature.auth.LoginScreen
import com.thevirtualtrust.ppis.feature.auth.OtpScreen
import com.thevirtualtrust.ppis.feature.auth.ResetPasswordOtpScreen
import com.thevirtualtrust.ppis.feature.auth.ResetPasswordSuccessScreen
import com.thevirtualtrust.ppis.feature.auth.ResetPasswordViewModel
import com.thevirtualtrust.ppis.feature.auth.SignupOtpScreen
import com.thevirtualtrust.ppis.feature.auth.SignupScreen
import com.thevirtualtrust.ppis.feature.auth.SignupViewModel
import com.thevirtualtrust.ppis.feature.auth.WelcomeScreen

@Composable
fun AuthNavigation(
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier
) {

    val navController =
        rememberNavController()

    val signupViewModel:
        SignupViewModel =
        hiltViewModel()

    val resetPasswordViewModel:
        ResetPasswordViewModel =
        hiltViewModel()

    NavHost(
        navController =
            navController,
        startDestination =
            AuthRoutes.WELCOME
    ) {

        composable(
            route =
                AuthRoutes.WELCOME
        ) {

            WelcomeScreen(
                modifier = modifier,
                onSignIn = {
                    navController.navigate(
                        AuthRoutes.LOGIN
                    )
                },
                onCreateAccount = {

                    signupViewModel
                        .resetAll()

                    navController.navigate(
                        AuthRoutes.SIGNUP
                    )
                }
            )
        }

        composable(
            route =
                AuthRoutes.LOGIN
        ) {

            LoginScreen(
                modifier = modifier,
                onBack = {
                    navController
                        .popBackStack()
                },
                onForgotPassword = {

                    resetPasswordViewModel
                        .resetAll()

                    navController.navigate(
                        AuthRoutes
                            .FORGOT_PASSWORD
                    )
                },
                onAuthenticated = {
                    onAuthenticated()
                },
                onOtpRequired = {
                        challengeId,
                        expiresInSeconds ->

                    navController.navigate(
                        AuthRoutes.otp(
                            purpose =
                                AuthOtpPurpose.LOGIN,
                            challengeId =
                                challengeId,
                            expiresInSeconds =
                                expiresInSeconds
                        )
                    )
                }
            )
        }

        composable(
            route =
                AuthRoutes.SIGNUP
        ) {

            SignupScreen(
                modifier = modifier,
                viewModel =
                    signupViewModel,
                onBack = {
                    signupViewModel
                        .resetAll()

                    navController
                        .popBackStack()
                },
                onOtpRequired = {
                    navController.navigate(
                        AuthRoutes.SIGNUP_OTP
                    )
                }
            )
        }

        composable(
            route =
                AuthRoutes.SIGNUP_OTP
        ) {

            SignupOtpScreen(
                modifier = modifier,
                viewModel =
                    signupViewModel,
                onAuthenticated = {
                    onAuthenticated()
                },
                onBack = {
                    navController
                        .popBackStack()
                }
            )
        }

        composable(
            route =
                AuthRoutes
                    .FORGOT_PASSWORD
        ) {

            ForgotPasswordScreen(
                modifier = modifier,
                viewModel =
                    resetPasswordViewModel,
                onBack = {

                    resetPasswordViewModel
                        .resetAll()

                    navController
                        .popBackStack()
                },
                onOtpRequired = {
                    navController.navigate(
                        AuthRoutes
                            .RESET_PASSWORD_OTP
                    )
                }
            )
        }

        composable(
            route =
                AuthRoutes
                    .RESET_PASSWORD_OTP
        ) {

            ResetPasswordOtpScreen(
                modifier = modifier,
                viewModel =
                    resetPasswordViewModel,
                onBack = {
                    navController
                        .popBackStack()
                },
                onPasswordReset = {

                    navController.navigate(
                        AuthRoutes
                            .RESET_PASSWORD_SUCCESS
                    ) {
                        popUpTo(
                            AuthRoutes
                                .FORGOT_PASSWORD
                        ) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route =
                AuthRoutes
                    .RESET_PASSWORD_SUCCESS
        ) {

            ResetPasswordSuccessScreen(
                modifier = modifier,
                onBackToLogin = {

                    resetPasswordViewModel
                        .resetAll()

                    navController.navigate(
                        AuthRoutes.LOGIN
                    ) {

                        popUpTo(
                            AuthRoutes.WELCOME
                        ) {
                            inclusive = false
                        }

                        launchSingleTop =
                            true
                    }
                }
            )
        }

        composable(
            route =
                AuthRoutes.OTP,
            arguments =
                listOf(
                    navArgument(
                        AuthRoutes
                            .OTP_PURPOSE_ARGUMENT
                    ) {
                        type =
                            NavType.StringType
                    },
                    navArgument(
                        AuthRoutes
                            .OTP_CHALLENGE_ARGUMENT
                    ) {
                        type =
                            NavType.StringType
                    },
                    navArgument(
                        AuthRoutes
                            .OTP_EXPIRES_ARGUMENT
                    ) {
                        type =
                            NavType.IntType
                    }
                )
        ) { backStackEntry ->

            val purpose =
                AuthOtpPurpose
                    .fromWireValue(
                        backStackEntry
                            .arguments
                            ?.getString(
                                AuthRoutes
                                    .OTP_PURPOSE_ARGUMENT
                            )
                    )
                    ?: return@composable

            OtpScreen(
                modifier = modifier,
                purpose = purpose,
                onAuthenticated = {
                    onAuthenticated()
                },
                onBack = {
                    navController
                        .popBackStack()
                }
            )
        }
    }
}
