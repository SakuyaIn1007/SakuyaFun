package com.sakuya.profileservices.viewmodel;

import com.sakuya.data.local.SessionManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SessionManager> sessionManagerProvider;

  public SettingsViewModel_Factory(Provider<SessionManager> sessionManagerProvider) {
    this.sessionManagerProvider = sessionManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(sessionManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<SessionManager> sessionManagerProvider) {
    return new SettingsViewModel_Factory(sessionManagerProvider);
  }

  public static SettingsViewModel newInstance(SessionManager sessionManager) {
    return new SettingsViewModel(sessionManager);
  }
}
