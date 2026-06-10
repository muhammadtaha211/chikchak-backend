import Chip from '@mui/material/Chip';
import {
  bookingStatusColor,
  fmtDateTime,
  fmtMoney,
  userStatusColor,
  yesNo,
} from '../format';

// Builders for DetailDialog rows ([label, value] pairs) per entity type.

export function technicianDetailRows(t) {
  if (!t) return [];
  return [
    ['ID', t.id],
    ['Name', t.user?.fullName],
    ['Email', t.user?.email],
    ['Phone', t.user?.phoneNumber],
    ['City', t.user?.city],
    ['Address', t.user?.address],
    ['Service type', t.serviceType],
    ['Verified', yesNo(t.isVerified)],
    ['Available', yesNo(t.isAvailable)],
    [
      'Account status',
      t.user?.status ? (
        <Chip size="small" label={t.user.status} color={userStatusColor(t.user.status)} />
      ) : null,
    ],
    ['Rating', t.averageRating != null ? `${t.averageRating} (${t.totalRatings} ratings)` : '—'],
    ['Total jobs', t.totalJobs],
    ['Years experience', t.yearsExperience],
    ['Diagnosis fee', fmtMoney(t.baseDiagnosisFee)],
    ['Service radius (km)', t.serviceRadius],
    ['Working hours', t.workingHours],
    ['Bio', t.bio],
    ['Registered', fmtDateTime(t.user?.createdAt)],
  ];
}

export function userDetailRows(u) {
  if (!u) return [];
  return [
    ['ID', u.id],
    ['Name', u.fullName],
    ['Email', u.email],
    ['Phone', u.phoneNumber],
    ['Role', u.role],
    [
      'Status',
      u.status ? <Chip size="small" label={u.status} color={userStatusColor(u.status)} /> : null,
    ],
    ['Language', u.preferredLanguage],
    ['Address', u.address],
    ['City', u.city],
    ['Account credits', fmtMoney(u.accountCredits)],
    ['Created', fmtDateTime(u.createdAt)],
  ];
}

export function bookingDetailRows(b) {
  if (!b) return [];
  return [
    ['ID', b.id],
    ['Booking #', b.bookingNumber],
    [
      'Status',
      b.status ? <Chip size="small" label={b.status} color={bookingStatusColor(b.status)} /> : null,
    ],
    ['Service type', b.serviceType],
    ['Urgency', b.urgency],
    ['Emergency', yesNo(b.isEmergency)],
    ['Customer', b.customer ? `${b.customer.fullName} (${b.customer.email})` : null],
    [
      'Technician',
      b.technician?.user ? `${b.technician.user.fullName} (${b.technician.user.email})` : null,
    ],
    ['Problem', b.problemDescription],
    ['Service address', b.serviceAddress],
    ['Preferred time', fmtDateTime(b.preferredDatetime)],
    ['Proposed time', fmtDateTime(b.proposedDatetime)],
    ['Scheduled time', fmtDateTime(b.scheduledDatetime)],
    ['Completed at', fmtDateTime(b.completedDatetime)],
    ['Cancelled at', fmtDateTime(b.cancelledDatetime)],
    ['Diagnosis fee', fmtMoney(b.diagnosisFee)],
    ['Service fee', fmtMoney(b.serviceFee)],
    ['Distance', b.distanceKm != null ? `${b.distanceKm} km (${fmtMoney(b.distanceFee)})` : null],
    ['Credits applied', fmtMoney(b.creditsApplied)],
    ['Total', fmtMoney(b.totalAmount)],
    ['Payment status', b.paymentStatus],
    ['Payment method', b.paymentMethod],
    ['Refund status', b.refundStatus],
    ['Customer notes', b.customerNotes],
    ['Technician notes', b.technicianNotes],
    ['Rated', yesNo(b.isRated)],
    ['Created', fmtDateTime(b.createdAt)],
  ];
}
