import { useEffect, useState } from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Chip from '@mui/material/Chip';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import CircularProgress from '@mui/material/CircularProgress';
import { api } from '../api/client';
import { useList } from '../useList';
import { useDetail } from '../useDetail';
import { useToast } from '../toast';
import Pager from '../components/Pager';
import ConfirmDialog from '../components/ConfirmDialog';
import DetailDialog from '../components/DetailDialog';
import TableStateRow from '../components/TableStateRow';
import { bookingDetailRows } from '../components/detailRows';
import { BOOKING_STATUSES, bookingStatusColor, fmtDateTime, fmtMoney } from '../format';

// Dialog for the admin "force status" escape hatch: pick a new status from the
// full BookingStatus enum, with an optional reason for the audit log.
function OverrideStatusDialog({ open, booking, busy, onConfirm, onClose }) {
  const [status, setStatus] = useState('');
  const [reason, setReason] = useState('');

  useEffect(() => {
    if (open) {
      setStatus(booking?.status || '');
      setReason('');
    }
  }, [open, booking]);

  return (
    <Dialog open={open} onClose={busy ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>Override booking status</DialogTitle>
      <DialogContent>
        <DialogContentText>
          Force booking {booking?.bookingNumber || `#${booking?.id}`} to a new status. This
          bypasses the normal state-transition rules — use with care.
        </DialogContentText>
        <TextField
          select
          fullWidth
          label="New status"
          value={status}
          onChange={(e) => setStatus(e.target.value)}
          sx={{ mt: 2 }}
        >
          {BOOKING_STATUSES.map((s) => (
            <MenuItem key={s} value={s}>
              {s}
            </MenuItem>
          ))}
        </TextField>
        <TextField
          fullWidth
          multiline
          minRows={2}
          label="Reason (optional)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          sx={{ mt: 2 }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button
          variant="contained"
          color="warning"
          disabled={busy || !status || status === booking?.status}
          startIcon={busy ? <CircularProgress size={16} color="inherit" /> : null}
          onClick={() => onConfirm(status, reason.trim() || undefined)}
        >
          Override
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default function BookingsPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [cancelTarget, setCancelTarget] = useState(null);
  const [overrideTarget, setOverrideTarget] = useState(null);
  const [actionBusy, setActionBusy] = useState(false);
  const showToast = useToast();
  const detail = useDetail();

  const { items, loading, error, reload } = useList(
    () => api('/api/admin/bookings', { params: { status, search, page, size } }),
    [status, search, page, size]
  );

  async function cancelBooking(reason) {
    setActionBusy(true);
    try {
      await api(`/api/admin/bookings/${cancelTarget.id}/cancel`, {
        method: 'POST',
        body: { reason },
      });
      showToast(`Booking ${cancelTarget.bookingNumber || cancelTarget.id} cancelled`);
      setCancelTarget(null);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setActionBusy(false);
    }
  }

  async function overrideStatus(newStatus, reason) {
    setActionBusy(true);
    try {
      await api(`/api/admin/bookings/${overrideTarget.id}/status`, {
        method: 'POST',
        body: { status: newStatus, reason },
      });
      showToast(`Booking ${overrideTarget.bookingNumber || overrideTarget.id} set to ${newStatus}`);
      setOverrideTarget(null);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setActionBusy(false);
    }
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Bookings
      </Typography>
      <Paper>
        <Stack
          direction="row"
          spacing={2}
          sx={{ p: 2 }}
          component="form"
          onSubmit={(e) => {
            e.preventDefault();
            setSearch(searchInput.trim());
            setPage(0);
          }}
        >
          <TextField
            select
            size="small"
            label="Status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            sx={{ width: 220 }}
          >
            <MenuItem value="">All</MenuItem>
            {BOOKING_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            size="small"
            label="Search"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            sx={{ width: 260 }}
          />
          <Button type="submit" variant="outlined">
            Search
          </Button>
        </Stack>

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Booking #</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Technician</TableCell>
                <TableCell>Service</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Scheduled</TableCell>
                <TableCell>Total</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow
                  colSpan={9}
                  loading={loading}
                  error={error}
                  emptyMessage="No bookings found"
                />
              ) : (
                items.map((b) => (
                  <TableRow key={b.id} hover>
                    <TableCell>{b.id}</TableCell>
                    <TableCell>{b.bookingNumber}</TableCell>
                    <TableCell>{b.customer?.fullName}</TableCell>
                    <TableCell>{b.technician?.user?.fullName || '—'}</TableCell>
                    <TableCell>{b.serviceType}</TableCell>
                    <TableCell>
                      <Chip size="small" label={b.status} color={bookingStatusColor(b.status)} />
                    </TableCell>
                    {/* Bookings start with a preferred time; scheduled is set once agreed. */}
                    <TableCell>{fmtDateTime(b.scheduledDatetime || b.preferredDatetime)}</TableCell>
                    <TableCell>{fmtMoney(b.totalAmount)}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          onClick={() => detail.show(() => api(`/api/admin/bookings/${b.id}`))}
                        >
                          View
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="warning"
                          onClick={() => setOverrideTarget(b)}
                        >
                          Override
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          disabled={b.status === 'CANCELLED'}
                          onClick={() => setCancelTarget(b)}
                        >
                          Cancel
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <Pager
          page={page}
          size={size}
          count={items.length}
          onPage={setPage}
          onSize={(s) => {
            setSize(s);
            setPage(0);
          }}
        />
      </Paper>

      <ConfirmDialog
        open={!!cancelTarget}
        title="Force-cancel booking?"
        message={`Cancel booking ${cancelTarget?.bookingNumber || `#${cancelTarget?.id}`}? This runs the normal cancellation side-effects and cannot be undone.`}
        confirmLabel="Force-cancel"
        withReason
        busy={actionBusy}
        onConfirm={cancelBooking}
        onClose={() => setCancelTarget(null)}
      />

      <OverrideStatusDialog
        open={!!overrideTarget}
        booking={overrideTarget}
        busy={actionBusy}
        onConfirm={overrideStatus}
        onClose={() => setOverrideTarget(null)}
      />

      <DetailDialog
        open={detail.open}
        title="Booking detail"
        loading={detail.loading}
        error={detail.error}
        rows={bookingDetailRows(detail.data)}
        onClose={detail.close}
      />
    </>
  );
}
