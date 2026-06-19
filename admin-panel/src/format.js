// Small display helpers shared across pages.

export function fmtDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString();
}

export function fmtMoney(value) {
  if (value === null || value === undefined) return '—';
  return `₪${value}`;
}

export function yesNo(value) {
  return value ? 'Yes' : 'No';
}

export function bookingStatusColor(status) {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'CANCELLED':
    case 'DISPUTED':
      return 'error';
    case 'PENDING':
    case 'TECHNICIAN_NOTIFIED':
    case 'TIME_DISCUSSION':
      return 'warning';
    default:
      return 'info';
  }
}

export function userStatusColor(status) {
  switch (status) {
    case 'ACTIVE':
      return 'success';
    case 'SUSPENDED':
      return 'error';
    case 'PENDING_VERIFICATION':
      return 'warning';
    default:
      return 'default';
  }
}

// Mirrors the backend enums (BookingStatus, UserRole, UserStatus).
export const BOOKING_STATUSES = [
  'PENDING',
  'TECHNICIAN_NOTIFIED',
  'TIME_DISCUSSION',
  'ACCEPTED',
  'SCHEDULED',
  'ON_THE_WAY',
  'ARRIVED',
  'IN_PROGRESS',
  'ASSESSMENT_COMPLETE',
  'COMPLETED',
  'CANCELLED',
  'DISPUTED',
];

export const USER_ROLES = ['CUSTOMER', 'TECHNICIAN', 'ADMIN'];

export const USER_STATUSES = ['ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION'];

// Mirrors the backend ReportStatus enum.
export const REPORT_STATUSES = ['OPEN', 'DISMISSED', 'ACTIONED'];

export function reportStatusColor(status) {
  switch (status) {
    case 'OPEN':
      return 'warning';
    case 'ACTIONED':
      return 'success';
    default:
      return 'default';
  }
}
