import '../features/auth/auth_repository.dart';
import '../features/auth/social_sign_in.dart';
import '../features/games/catalogue_repository.dart';
import 'network/api_client.dart';
import 'storage/session_store.dart';

/// Minimal service locator.
class Services {
  Services._(this.store, this.api, this.auth, this.catalogue);

  final SessionStore store;
  final ApiClient api;
  final AuthRepository auth;
  final CatalogueRepository catalogue;

  static late Services instance;

  static Future<void> init() async {
    final store = await SessionStore.create();
    final api = ApiClient();
    final auth = AuthRepository(api, store, SocialSignIn());
    instance = Services._(store, api, auth, CatalogueRepository(api));
  }
}

Services get services => Services.instance;
