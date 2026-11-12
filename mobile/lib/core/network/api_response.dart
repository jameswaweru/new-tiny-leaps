/// Mirrors the backend envelope: { statusCode, customerMessage, description, data, pagination }.
class ApiResponse {
  ApiResponse({
    required this.statusCode,
    required this.customerMessage,
    this.description,
    this.data,
    this.pagination,
  });

  final int statusCode;
  final String customerMessage;
  final String? description;
  final dynamic data;
  final Pagination? pagination;

  factory ApiResponse.fromJson(Map<String, dynamic> json) => ApiResponse(
        statusCode: (json['statusCode'] as num?)?.toInt() ?? 0,
        customerMessage: json['customerMessage'] as String? ?? '',
        description: json['description'] as String?,
        data: json['data'],
        pagination: json['pagination'] is Map<String, dynamic>
            ? Pagination.fromJson(json['pagination'] as Map<String, dynamic>)
            : null,
      );
}

class Pagination {
  Pagination({required this.page, required this.size, required this.totalElements, required this.totalPages, required this.hasNext});

  final int page;
  final int size;
  final int totalElements;
  final int totalPages;
  final bool hasNext;

  factory Pagination.fromJson(Map<String, dynamic> j) => Pagination(
        page: (j['page'] as num?)?.toInt() ?? 0,
        size: (j['size'] as num?)?.toInt() ?? 0,
        totalElements: (j['totalElements'] as num?)?.toInt() ?? 0,
        totalPages: (j['totalPages'] as num?)?.toInt() ?? 0,
        hasNext: j['hasNext'] as bool? ?? false,
      );
}

class ApiException implements Exception {
  ApiException(this.statusCode, this.customerMessage, [this.description]);

  /// 0 means the request never got a response (offline, timeout...).
  final int statusCode;
  final String customerMessage;
  final String? description;

  bool get isUnauthorized => statusCode == 401;
  bool get isNetwork => statusCode == 0;

  static ApiException network() =>
      ApiException(0, "We can't reach Tiny Leaps right now. Check your internet and try again.", 'NETWORK_ERROR');

  @override
  String toString() => 'ApiException($statusCode, $customerMessage, $description)';
}
