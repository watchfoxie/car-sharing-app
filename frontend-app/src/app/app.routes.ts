import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';

export const routes: Routes = [
	{
		path: '',
		pathMatch: 'full',
		redirectTo: 'cars'
	},
	{
		path: 'login',
		loadComponent: () =>
			import('./features/auth/login/login').then((m) => m.Login)
	},
	{
		path: 'auth/callback',
		loadComponent: () =>
			import('./features/auth/callback/callback').then((m) => m.Callback)
	},
	{
		path: 'cars',
		loadComponent: () =>
			import('./features/cars/car-list/car-list').then((m) => m.CarList)
	},
	{
		path: 'cars/:id',
		loadComponent: () =>
			import('./features/cars/car-detail/car-detail').then((m) => m.CarDetail)
	},
	{
		path: 'booking',
		canActivate: [authGuard, roleGuard],
		data: { roles: ['RENTER'] },
		loadComponent: () =>
			import('./features/booking/booking-form/booking-form').then((m) => m.BookingForm)
	},
	{
		path: 'dashboard',
		canActivate: [authGuard],
		children: [
			{
				path: 'owner',
				canActivate: [roleGuard],
				data: { roles: ['OWNER'] },
				loadComponent: () =>
					import('./features/dashboard/owner-dashboard/owner-dashboard').then((m) => m.OwnerDashboard)
			},
			{
				path: 'renter',
				canActivate: [roleGuard],
				data: { roles: ['RENTER'] },
				loadComponent: () =>
					import('./features/dashboard/renter-dashboard/renter-dashboard').then((m) => m.RenterDashboard)
			},
			{
				path: '',
				pathMatch: 'full',
				redirectTo: 'renter'
			}
		]
	},
	{
		path: '**',
		redirectTo: 'cars'
	}
];
