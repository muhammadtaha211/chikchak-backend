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
import { useList } from '../useList';
import { useDetail } from '../useDetail';
import Pager from '../components/Pager';
import DetailDialog from '../components/DetailDialog';
import TableStateRow from '../components/TableStateRow';
import { technicianDetailRows } from '../components/detailRows';
import { fmtMoney, yesNo } from '../format';

export default function TechniciansPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [verified, setVerified] = useState(''); // '' | 'true' | 'false'
  const [searchInput, setSearchInput] = useState('');
  const [search, setSearch] = useState(''); // applied on submit, not per keystroke
  const detail = useDetail();

  const { items, loading, error } = useList(
    () => api('/api/admin/technicians', { params: { verified, search, page, size } }),
    [verified, search, page, size]
  );

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        All Technicians
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
            label="Verified"
            value={verified}
            onChange={(e) => {
              setVerified(e.target.value);
              setPage(0);
            }}
            sx={{ width: 150 }}
          >
            <MenuItem value="">All</MenuItem>
            <MenuItem value="true">Verified</MenuItem>
            <MenuItem value="false">Unverified</MenuItem>
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
                <TableCell>Service</TableCell>
                <TableCell>Verified</TableCell>
                <TableCell>Available</TableCell>
                <TableCell>Rating</TableCell>
                <TableCell>Jobs</TableCell>
                <TableCell>Diagnosis fee</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow
                  colSpan={10}
                  loading={loading}
                  error={error}
                  emptyMessage="No technicians found"
                />
              ) : (
                items.map((t) => (
                  <TableRow key={t.id} hover>
                    <TableCell>{t.id}</TableCell>
                    <TableCell>{t.user?.fullName}</TableCell>
                    <TableCell>{t.user?.email}</TableCell>
                    <TableCell>{t.serviceType}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={t.isVerified ? 'Verified' : 'Unverified'}
                        color={t.isVerified ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>{yesNo(t.isAvailable)}</TableCell>
                    <TableCell>
                      {t.averageRating != null ? `${t.averageRating} (${t.totalRatings})` : '—'}
                    </TableCell>
                    <TableCell>{t.totalJobs}</TableCell>
                    <TableCell>{fmtMoney(t.baseDiagnosisFee)}</TableCell>
                    <TableCell align="right">
                      <Button
                        size="small"
                        onClick={() => detail.show(() => api(`/api/admin/technicians/${t.id}`))}
                      >
                        View
                      </Button>
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
