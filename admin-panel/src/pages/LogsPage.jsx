import { useState } from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import { api } from '../api/client';
import { useList } from '../useList';
import Pager from '../components/Pager';
import TableStateRow from '../components/TableStateRow';
import { fmtDateTime } from '../format';

// Read-only audit trail of state-changing admin actions (newest first).
export default function LogsPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { items, loading, error } = useList(
    () => api('/api/admin/logs', { params: { page, size } }),
    [page, size]
  );

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Audit Log
      </Typography>
      <Paper>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>When</TableCell>
                <TableCell>Admin</TableCell>
                <TableCell>Action</TableCell>
                <TableCell>Target</TableCell>
                <TableCell>Details</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow
                  colSpan={6}
                  loading={loading}
                  error={error}
                  emptyMessage="No admin actions logged yet"
                />
              ) : (
                items.map((log) => (
                  <TableRow key={log.id} hover>
                    <TableCell>{log.id}</TableCell>
                    <TableCell sx={{ whiteSpace: 'nowrap' }}>{fmtDateTime(log.createdAt)}</TableCell>
                    <TableCell>{log.adminEmail}</TableCell>
                    <TableCell>{log.actionType}</TableCell>
                    <TableCell>
                      {log.targetType}
                      {log.targetId != null ? ` #${log.targetId}` : ''}
                    </TableCell>
                    <TableCell sx={{ maxWidth: 400, wordBreak: 'break-word' }}>
                      {log.details}
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
    </>
  );
}
