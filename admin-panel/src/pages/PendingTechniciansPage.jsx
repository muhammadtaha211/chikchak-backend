import { useState } from 'react';
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
import { api } from '../api/client';
import { useList } from '../useList';
import { useDetail } from '../useDetail';
import { useToast } from '../toast';
import Pager from '../components/Pager';
import ConfirmDialog from '../components/ConfirmDialog';
import DetailDialog from '../components/DetailDialog';
import TableStateRow from '../components/TableStateRow';
import { technicianDetailRows } from '../components/detailRows';
import { fmtDateTime, fmtMoney } from '../format';

export default function PendingTechniciansPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [busyId, setBusyId] = useState(null); // technician id with an action in flight
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectBusy, setRejectBusy] = useState(false);
  const showToast = useToast();
  const detail = useDetail();

  const { items, loading, error, reload } = useList(
    () => api('/api/admin/technicians/pending', { params: { page, size } }),
    [page, size]
  );

  async function approve(t) {
    setBusyId(t.id);
    try {
      await api(`/api/admin/technicians/${t.id}/approve`, { method: 'POST' });
      showToast(`${t.user?.fullName || 'Technician'} approved`);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setBusyId(null);
    }
  }

  async function reject(reason) {
    setRejectBusy(true);
    try {
      await api(`/api/admin/technicians/${rejectTarget.id}/reject`, {
        method: 'POST',
        body: { reason },
      });
      showToast(`${rejectTarget.user?.fullName || 'Technician'} rejected`);
      setRejectTarget(null);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setRejectBusy(false);
    }
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Technicians — Pending Approval
      </Typography>
      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Service</TableCell>
                <TableCell>Diagnosis fee</TableCell>
                <TableCell>Registered</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow
                  colSpan={8}
                  loading={loading}
                  error={error}
                  emptyMessage="No technicians awaiting approval"
                />
              ) : (
                items.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell>{t.id}</TableCell>
                    <TableCell>{t.user?.fullName}</TableCell>
                    <TableCell>{t.user?.email}</TableCell>
                    <TableCell>{t.user?.phoneNumber}</TableCell>
                    <TableCell>{t.serviceType}</TableCell>
                    <TableCell>{fmtMoney(t.baseDiagnosisFee)}</TableCell>
                    <TableCell>{fmtDateTime(t.user?.createdAt)}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          onClick={() => detail.show(() => api(`/api/admin/technicians/${t.id}`))}
                        >
                          View
                        </Button>
                        <Button
                          size="small"
                          variant="contained"
                          color="success"
                          disabled={busyId === t.id}
                          onClick={() => approve(t)}
                        >
                          Approve
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          color="error"
                          disabled={busyId === t.id}
                          onClick={() => setRejectTarget(t)}
                        >
                          Reject
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
        open={!!rejectTarget}
        title="Reject technician?"
        message={`Reject ${rejectTarget?.user?.fullName || 'this technician'}'s application? They will remain unverified.`}
        confirmLabel="Reject"
        withReason
        busy={rejectBusy}
        onConfirm={reject}
        onClose={() => setRejectTarget(null)}
      />

      <DetailDialog
        open={detail.open}
        title="Technician detail"
        loading={detail.loading}
        error={detail.error}
        rows={technicianDetailRows(detail.data)}
        onClose={detail.close}
      />
    </>
  );
}
