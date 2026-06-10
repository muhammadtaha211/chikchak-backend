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
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Chip from '@mui/material/Chip';
import { api } from '../api/client';
import { useAuth } from '../auth';
import { useList } from '../useList';
import { useDetail } from '../useDetail';
import { useToast } from '../toast';
import Pager from '../components/Pager';
import ConfirmDialog from '../components/ConfirmDialog';
import DetailDialog from '../components/DetailDialog';
import TableStateRow from '../components/TableStateRow';
import { userDetailRows } from '../components/detailRows';
import { fmtDateTime, fmtMoney, USER_ROLES, USER_STATUSES, userStatusColor } from '../format';

export default function UsersPage() {
  const { user: me } = useAuth();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [role, setRole] = useState('');
  const [status, setStatus] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [suspendTarget, setSuspendTarget] = useState(null);
  const [suspendBusy, setSuspendBusy] = useState(false);
  const showToast = useToast();
  const detail = useDetail();

  const { items, loading, error, reload } = useList(
    () => api('/api/admin/users', { params: { role, status, search, page, size } }),
    [role, status, search, page, size]
  );

  async function suspend(reason) {
    setSuspendBusy(true);
    try {
      await api(`/api/admin/users/${suspendTarget.id}/suspend`, {
        method: 'POST',
        body: { reason },
      });
      showToast(`${suspendTarget.fullName || 'User'} suspended`);
      setSuspendTarget(null);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setSuspendBusy(false);
    }
  }

  async function reactivate(u) {
    setBusyId(u.id);
    try {
      await api(`/api/admin/users/${u.id}/reactivate`, { method: 'POST' });
      showToast(`${u.fullName || 'User'} reactivated`);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setBusyId(null);
    }
  }

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Users
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
            label="Role"
            value={role}
            onChange={(e) => {
              setRole(e.target.value);
              setPage(0);
            }}
            sx={{ width: 160 }}
          >
            <MenuItem value="">All</MenuItem>
            {USER_ROLES.map((r) => (
              <MenuItem key={r} value={r}>
                {r}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            select
            size="small"
            label="Status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            sx={{ width: 200 }}
          >
            <MenuItem value="">All</MenuItem>
            {USER_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
          <TextField
            size="small"
            label="Search name or email"
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
                <TableCell>Name</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Phone</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Credits</TableCell>
                <TableCell>Created</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow colSpan={9} loading={loading} error={error} emptyMessage="No users found" />
              ) : (
                items.map((u) => (
                  <TableRow key={u.id} hover>
                    <TableCell>{u.id}</TableCell>
                    <TableCell>{u.fullName}</TableCell>
                    <TableCell>{u.email}</TableCell>
                    <TableCell>{u.phoneNumber}</TableCell>
                    <TableCell>{u.role}</TableCell>
                    <TableCell>
                      <Chip size="small" label={u.status} color={userStatusColor(u.status)} />
                    </TableCell>
                    <TableCell>{fmtMoney(u.accountCredits)}</TableCell>
                    <TableCell>{fmtDateTime(u.createdAt)}</TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          onClick={() => detail.show(() => api(`/api/admin/users/${u.id}`))}
                        >
                          View
                        </Button>
                        {u.status === 'SUSPENDED' ? (
                          <Button
                            size="small"
                            variant="outlined"
                            color="success"
                            disabled={busyId === u.id}
                            onClick={() => reactivate(u)}
                          >
                            Reactivate
                          </Button>
                        ) : (
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            // Don't allow suspending your own admin account.
                            disabled={busyId === u.id || u.id === me?.id}
                            onClick={() => setSuspendTarget(u)}
                          >
                            Suspend
                          </Button>
                        )}
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
        open={!!suspendTarget}
        title="Suspend user?"
        message={`Suspend ${suspendTarget?.fullName || 'this user'} (${suspendTarget?.email || ''})? They will no longer be able to log in.`}
        confirmLabel="Suspend"
        withReason
        busy={suspendBusy}
        onConfirm={suspend}
        onClose={() => setSuspendTarget(null)}
      />

      <DetailDialog
        open={detail.open}
        title="User detail"
        loading={detail.loading}
        error={detail.error}
        rows={userDetailRows(detail.data)}
        onClose={detail.close}
      />
    </>
  );
}
