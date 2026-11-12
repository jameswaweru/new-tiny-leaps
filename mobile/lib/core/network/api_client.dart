import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http/http.dart' as http;
import 'package:uuid/uuid.dart';

import '../config/app_config.dart';
import '../storage/session.dart';
import 'api_response.dart';

/// Supplies a valid session for authenticated calls (implemented by AuthRepository).
abstract class SessionProvider {
  Future<Session> validSession({bool forceRefresh = false});
}

/// Thin HTTP client for the Tiny Leaps gateway. Every response is the unified envelope.
class ApiClient {
  ApiClient({http.Client? client, String? baseUrl})
      : _http = client ?? http.Client(),
        baseUrl = baseUrl ?? AppConfig.apiBaseUrl;

  final http.Client _http;
  final String baseUrl;
  SessionProvider? sessionProvider;
  static const _uuid = Uuid();

  Future<ApiResponse> get(String path, {Map<String, String>? query, bool auth = true}) =>
      _send('GET', path, query: query, auth: auth);

  Future<ApiResponse> post(String path, {Object? body, bool auth = true}) =>
      _send('POST', path, body: body, auth: auth);

  Future<ApiResponse> _send(String method, String path,
      {Map<String, String>? query, Object? body, required bool auth, bool retried = false}) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    final headers = <String, String>{
      'Accept': 'application/json',
      'Content-Type': 'application/json',
      'X-Request-Id': _uuid.v4(),
    };
    if (auth) {
      final provider = sessionProvider;
      if (provider == null) throw StateError('ApiClient.sessionProvider not set');
      final session = await provider.validSession();
      headers.addAll(session.authHeaders);
    }

    http.Response res;
    try {
      final req = http.Request(method, uri)..headers.addAll(headers);
      if (body != null) req.body = jsonEncode(body);
      res = await http.Response.fromStream(await _http.send(req).timeout(AppConfig.requestTimeout));
    } on SocketException {
      throw ApiException.network();
    } on TimeoutException {
      throw ApiException.network();
    } on http.ClientException {
      throw ApiException.network();
    }

    if (res.statusCode == 401 && auth && !retried) {
      // Access token rejected (expired early, revoked...). Refresh once and retry.
      await sessionProvider!.validSession(forceRefresh: true);
      return _send(method, path, query: query, body: body, auth: auth, retried: true);
    }

    ApiResponse envelope;
    try {
      envelope = ApiResponse.fromJson(jsonDecode(utf8.decode(res.bodyBytes)) as Map<String, dynamic>);
    } catch (_) {
      throw ApiException(res.statusCode, 'Oops! Something went wrong. Please try again.', 'INVALID_RESPONSE');
    }
    if (res.statusCode < 200 || res.statusCode >= 300) {
      throw ApiException(res.statusCode, envelope.customerMessage, envelope.description);
    }
    return envelope;
  }
}
